package com.example.keycloak.authenticator;

import com.example.keycloak.authenticator.utils.OtpUtils;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;

/**
 * NovaPulse — Stage 1: Username + Password Authenticator (1FA).
 *
 * <p>
 * Validates the user's username and password. On success it:
 * <ol>
 * <li>Resolves and sets the user on the auth-context.</li>
 * <li>Generates a 6-digit OTP, stores it in the auth session, and sends it
 * to the user's email — so the next step ({@code EmailOtpAuthenticator})
 * only needs to render the form and verify.</li>
 * <li>Calls {@code context.success()} to advance the flow.</li>
 * </ol>
 *
 * <p>
 * Add this as the <strong>first REQUIRED step</strong> in your Keycloak
 * authentication flow, followed by {@link EmailOtpAuthenticator}.
 */
public class UsernamePasswordAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(UsernamePasswordAuthenticator.class);

    // ── Form field names (must match FreeMarker templates) ───────────────────
    static final String FIELD_USERNAME = "username";
    static final String FIELD_PASSWORD = "password";

    // ─────────────────────────────────────────────────────────────────────────
    // Authenticator lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /** Present the username/password login form. */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.debugf("[NovaPulse 1FA] Presenting login form — session %s",
                context.getAuthenticationSession().getParentSession().getId());
        context.challenge(createLoginForm(context, null));
    }

    /** Handle the POST submission of the login form. */
    @Override
    public void action(AuthenticationFlowContext context) {
        handleCredentials(context);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Credential validation
    // ─────────────────────────────────────────────────────────────────────────

    private void handleCredentials(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String username = formData.getFirst(FIELD_USERNAME);
        String password = formData.getFirst(FIELD_PASSWORD);

        // ── 1. Blank-field validation ─────────────────────────────────────────
        if (isBlank(username)) {
            LOG.warn("[NovaPulse 1FA] Login attempt with blank username");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, Messages.MISSING_USERNAME));
            return;
        }

        if (isBlank(password)) {
            LOG.warnf("[NovaPulse 1FA] Login attempt with blank password for user: %s", username);
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    createErrorForm(context, Messages.MISSING_PASSWORD));
            return;
        }

        // ── 2. Resolve user ───────────────────────────────────────────────────
        context.getEvent().detail(Details.USERNAME, username);
        RealmModel realm = context.getRealm();
        UserModel user = context.getSession().users().getUserByUsername(realm, username);

        if (user == null && realm.isLoginWithEmailAllowed()) {
            user = context.getSession().users().getUserByEmail(realm, username);
        }

        if (user == null) {
            LOG.infof("[NovaPulse 1FA] User not found: %s", username);
            auditFailure(context, username, "USER_NOT_FOUND");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, Messages.INVALID_USER));
            return;
        }

        // ── 3. Disabled account ───────────────────────────────────────────────
        if (!user.isEnabled()) {
            LOG.infof("[NovaPulse 1FA] Account disabled: %s", username);
            auditFailure(context, username, "ACCOUNT_DISABLED");
            context.getEvent().user(user).error(Errors.USER_DISABLED);
            context.failureChallenge(
                    AuthenticationFlowError.USER_DISABLED,
                    createErrorForm(context, Messages.ACCOUNT_DISABLED));
            return;
        }

        // ── 4. Brute-force protection ─────────────────────────────────────────
        if (realm.isBruteForceProtected() && context.getProtector() != null) {
            if (context.getProtector().isPermanentlyLockedOut(context.getSession(), realm, user)) {
                LOG.warnf("[NovaPulse 1FA] Account permanently locked: %s", username);
                auditFailure(context, username, "PERMANENTLY_LOCKED");
                context.getEvent().user(user).error(Errors.USER_TEMPORARILY_DISABLED);
                context.failureChallenge(
                        AuthenticationFlowError.USER_TEMPORARILY_DISABLED,
                        createErrorForm(context, Messages.ACCOUNT_DISABLED));
                return;
            }
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), realm, user)) {
                LOG.warnf("[NovaPulse 1FA] Account temporarily locked: %s", username);
                auditFailure(context, username, "TEMPORARILY_LOCKED");
                context.getEvent().user(user).error(Errors.USER_TEMPORARILY_DISABLED);
                context.failureChallenge(
                        AuthenticationFlowError.USER_TEMPORARILY_DISABLED,
                        createErrorForm(context, Messages.ACCOUNT_TEMPORARILY_DISABLED));
                return;
            }
        }

        // ── 5. Validate password ──────────────────────────────────────────────
        boolean valid = user.credentialManager()
                .isValid(UserCredentialModel.password(password));

        if (!valid) {
            LOG.infof("[NovaPulse 1FA] Invalid password for user: %s", username);
            auditFailure(context, username, "INVALID_PASSWORD");

            if (realm.isBruteForceProtected() && context.getProtector() != null) {
                context.getProtector().failedLogin(realm, user, context.getConnection());
            }

            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    createErrorForm(context, Messages.INVALID_USER));
            return;
        }

        // ── 6. Password OK → check email ──────────────────────────────────────
        String email = user.getEmail();
        if (isBlank(email)) {
            LOG.errorf("[NovaPulse 1FA] User %s has no email address; cannot send OTP", username);
            auditFailure(context, username, "NO_EMAIL");
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, "noEmailConfigured"));
            return;
        }

        // ── 7. Generate + send OTP so the next step can verify it ────────────
        LOG.infof("[NovaPulse 1FA] Password valid for user: %s — generating OTP", username);
        String otp = OtpUtils.generateAndStore(context);
        OtpUtils.sendOtpEmail(context.getSession(), realm, user, otp);

        // Persist the resolved user so the 2FA step can access it without a DB hit
        context.setUser(user);

        // Advance to the next authenticator in the flow
        context.success();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Form helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Response createLoginForm(AuthenticationFlowContext context, FormMessage error) {
        var form = context.form().setAttribute("realm", context.getRealm());
        if (error != null)
            form.addError(error);
        return form.createForm("novapulse-login.ftl");
    }

    private Response createErrorForm(AuthenticationFlowContext context, String messageKey) {
        return context.form()
                .setAttribute("realm", context.getRealm())
                .addError(new FormMessage(messageKey))
                .createForm("novapulse-login.ftl");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void auditFailure(AuthenticationFlowContext context, String username, String reason) {
        LOG.warnf("[NovaPulse 1FA AUDIT] FAILURE | realm=%s | user=%s | reason=%s | ip=%s",
                context.getRealm().getName(), username, reason,
                context.getConnection().getRemoteAddr());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Required interface stubs
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
