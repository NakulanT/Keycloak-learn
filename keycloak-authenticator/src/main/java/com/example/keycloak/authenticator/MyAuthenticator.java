package com.example.keycloak.authenticator;

import com.example.keycloak.authenticator.utils.AuthUtils;

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
 * NovaPulse Custom Username/Password + Email OTP Authenticator.
 *
 * <p>
 * Two-stage flow within a single execution step:
 * <ol>
 * <li>Stage "credentials" — validate username + password.</li>
 * <li>Stage "otp" — send OTP to user's email and verify it.</li>
 * </ol>
 *
 * <p>
 * Stage is tracked via the auth-session note {@value #NOTE_STAGE}.
 */
public class MyAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(MyAuthenticator.class);

    // ── Form field names (must match FreeMarker templates) ───────────────────
    static final String FIELD_USERNAME = "username";
    static final String FIELD_PASSWORD = "password";
    static final String FIELD_OTP = "otp";

    // ── Auth-session notes ────────────────────────────────────────────────────
    /** Tracks which stage we are in: "credentials" or "otp". */
    static final String NOTE_STAGE = "novapulse_stage";
    static final String STAGE_CREDS = "credentials";
    static final String STAGE_OTP = "otp";

    // ─────────────────────────────────────────────────────────────────────────
    // Authenticator lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * First visit — always render the username/password form.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.debugf("[NovaPulse] Presenting login form — session %s",
                context.getAuthenticationSession().getParentSession().getId());

        // Ensure we start at the credentials stage
        context.getAuthenticationSession().setAuthNote(NOTE_STAGE, STAGE_CREDS);
        context.challenge(createLoginForm(context, null));
    }

    /**
     * Called on every POST submission. Dispatches to the correct stage handler.
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        String stage = context.getAuthenticationSession().getAuthNote(NOTE_STAGE);

        if (STAGE_OTP.equals(stage)) {
            handleOtpSubmission(context);
        } else {
            handleCredentials(context);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stage 1 — Username + Password
    // ─────────────────────────────────────────────────────────────────────────

    private void handleCredentials(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String username = formData.getFirst(FIELD_USERNAME);
        String password = formData.getFirst(FIELD_PASSWORD);

        // ── 1. Blank-field validation ─────────────────────────────────────────
        if (isBlank(username)) {
            LOG.warn("[NovaPulse] Login attempt with blank username");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, Messages.MISSING_USERNAME));
            return;
        }

        if (isBlank(password)) {
            LOG.warnf("[NovaPulse] Login attempt with blank password for user: %s", username);
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
            LOG.infof("[NovaPulse] User not found: %s", username);
            auditFailure(context, username, "USER_NOT_FOUND");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, Messages.INVALID_USER));
            return;
        }

        // ── 3. Disabled account ───────────────────────────────────────────────
        if (!user.isEnabled()) {
            LOG.infof("[NovaPulse] Account disabled: %s", username);
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
                LOG.warnf("[NovaPulse] Account permanently locked: %s", username);
                auditFailure(context, username, "PERMANENTLY_LOCKED");
                context.getEvent().user(user).error(Errors.USER_TEMPORARILY_DISABLED);
                context.failureChallenge(
                        AuthenticationFlowError.USER_TEMPORARILY_DISABLED,
                        createErrorForm(context, Messages.ACCOUNT_DISABLED));
                return;
            }
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), realm, user)) {
                LOG.warnf("[NovaPulse] Account temporarily locked: %s", username);
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
            LOG.infof("[NovaPulse] Invalid password for user: %s", username);
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

        // ── 6. Password OK → send OTP and advance to stage 2 ─────────────────
        LOG.infof("[NovaPulse] Password valid for user: %s — advancing to OTP stage", username);

        String email = user.getEmail();
        if (isBlank(email)) {
            // Cannot send OTP without an email — fail safe
            LOG.errorf("[NovaPulse] User %s has no email address; cannot send OTP", username);
            auditFailure(context, username, "NO_EMAIL");
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    createErrorForm(context, "noEmailConfigured")); // custom message key
            return;
        }

        // Generate and send OTP; store it in the auth session
        String otp = AuthUtils.generateAndStore(context);
        AuthUtils.sendOtpEmail(context.getSession(), realm, user, otp);

        // Persist the resolved user so stage 2 can retrieve it without a second DB hit
        context.setUser(user);
        context.getAuthenticationSession().setAuthNote(NOTE_STAGE, STAGE_OTP);

        context.challenge(createOtpForm(context, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stage 2 — Email OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void handleOtpSubmission(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String submittedOtp = formData.getFirst(FIELD_OTP);

        // ── 1. Blank OTP guard ────────────────────────────────────────────────
        if (isBlank(submittedOtp)) {
            LOG.warn("[NovaPulse] OTP submission is blank");
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    createOtpForm(context, "missingOtp"));
            return;
        }

        // ── 2. Validate OTP (checks value + expiry) ───────────────────────────
        AuthUtils.ValidationResult result = AuthUtils.validate(context, submittedOtp);

        switch (result) {
            case VALID -> {
                LOG.infof("[NovaPulse] OTP valid for user: %s",
                        context.getUser().getUsername());
                auditSuccess(context, context.getUser().getUsername());

                // Reset brute-force counter on full success
                if (context.getRealm().isBruteForceProtected()
                        && context.getProtector() != null) {
                    context.getProtector().successfulLogin(
                            context.getRealm(),
                            context.getUser(),
                            context.getConnection());
                }

                context.success();
            }

            case EXPIRED -> {
                LOG.infof("[NovaPulse] OTP expired for user: %s",
                        context.getUser().getUsername());
                auditFailure(context, context.getUser().getUsername(), "OTP_EXPIRED");
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_CREDENTIALS,
                        createOtpForm(context, "otpExpired"));
            }

            case INVALID -> {
                LOG.infof("[NovaPulse] OTP mismatch for user: %s",
                        context.getUser().getUsername());
                auditFailure(context, context.getUser().getUsername(), "OTP_INVALID");
                context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_CREDENTIALS,
                        createOtpForm(context, "invalidOtp"));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Form helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Response createLoginForm(AuthenticationFlowContext context, FormMessage error) {
        var form = context.form().setAttribute("realm", context.getRealm());
        if (error != null)
            form.addError(error);
        return form.createForm("my-custom-form.ftl");
    }

    private Response createErrorForm(AuthenticationFlowContext context, String messageKey) {
        return context.form()
                .setAttribute("realm", context.getRealm())
                .addError(new FormMessage(messageKey))
                .createForm("my-custom-form.ftl");
    }

    /**
     * Renders the OTP input page.
     *
     * @param messageKey nullable — if non-null, an error message is shown
     */
    private Response createOtpForm(AuthenticationFlowContext context, String messageKey) {
        var form = context.form()
                .setAttribute("realm", context.getRealm())
                // Expose masked email so the template can show "Check your inbox at
                // j***@acme.com"
                .setAttribute("maskedEmail",
                        maskEmail(context.getUser() != null
                                ? context.getUser().getEmail()
                                : ""));
        if (messageKey != null)
            form.addError(new FormMessage(messageKey));
        return form.createForm("login-otp.ftl");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit log helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void auditFailure(AuthenticationFlowContext context, String username, String reason) {
        LOG.warnf("[NovaPulse AUDIT] FAILURE | realm=%s | user=%s | reason=%s | ip=%s",
                context.getRealm().getName(), username, reason,
                context.getConnection().getRemoteAddr());
    }

    private void auditSuccess(AuthenticationFlowContext context, String username) {
        LOG.infof("[NovaPulse AUDIT] SUCCESS | realm=%s | user=%s | ip=%s",
                context.getRealm().getName(), username,
                context.getConnection().getRemoteAddr());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Masks an email for display: {@code john.doe@acme.com} →
     * {@code j*******@acme.com}.
     */
    private String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@"))
            return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at); // includes '@'
        if (local.length() <= 1)
            return local + domain;
        return local.charAt(0) + "*".repeat(local.length() - 1) + domain;
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