package com.example.keycloak.authenticator;

import com.example.keycloak.authenticator.utils.OtpUtils;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

/**
 * NovaPulse — Stage 2: Email OTP Authenticator (2FA).
 *
 * <p>
 * This step assumes the OTP has already been generated and emailed by the
 * preceding {@link UsernamePasswordAuthenticator}. It simply renders the OTP
 * input form and verifies the submitted code.
 *
 * <p>
 * Add this as the <strong>second REQUIRED step</strong> in your Keycloak
 * authentication flow, immediately after {@link UsernamePasswordAuthenticator}.
 */
public class EmailOtpAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(EmailOtpAuthenticator.class);

    // ── Form field name (must match login-otp.ftl) ───────────────────────────
    static final String FIELD_OTP = "otp";

    // ─────────────────────────────────────────────────────────────────────────
    // Authenticator lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Present the OTP input form. The OTP was already sent by the preceding
     * 1FA step, so we only need to render the challenge page.
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.debugf("[NovaPulse 2FA] Presenting OTP form — session %s",
                context.getAuthenticationSession().getParentSession().getId());
        context.challenge(createOtpForm(context, null));
    }

    /** Handle the POST submission of the OTP form. */
    @Override
    public void action(AuthenticationFlowContext context) {
        handleOtpSubmission(context);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP verification
    // ─────────────────────────────────────────────────────────────────────────

    private void handleOtpSubmission(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String submittedOtp = formData.getFirst(FIELD_OTP);

        // ── 1. Blank OTP guard ────────────────────────────────────────────────
        if (isBlank(submittedOtp)) {
            LOG.warn("[NovaPulse 2FA] OTP submission is blank");
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    createOtpForm(context, "missingOtp"));
            return;
        }

        // ── 2. Validate OTP (checks value + expiry) ───────────────────────────
        OtpUtils.ValidationResult result = OtpUtils.validate(context, submittedOtp);

        switch (result) {
            case VALID -> {
                LOG.infof("[NovaPulse 2FA] OTP valid for user: %s",
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
                LOG.infof("[NovaPulse 2FA] OTP expired for user: %s",
                        context.getUser().getUsername());
                auditFailure(context, context.getUser().getUsername(), "OTP_EXPIRED");
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_CREDENTIALS,
                        createOtpForm(context, "otpExpired"));
            }

            case INVALID -> {
                LOG.infof("[NovaPulse 2FA] OTP mismatch for user: %s",
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
    // Form helper
    // ─────────────────────────────────────────────────────────────────────────

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
    // Audit helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void auditFailure(AuthenticationFlowContext context, String username, String reason) {
        LOG.warnf("[NovaPulse 2FA AUDIT] FAILURE | realm=%s | user=%s | reason=%s | ip=%s",
                context.getRealm().getName(), username, reason,
                context.getConnection().getRemoteAddr());
    }

    private void auditSuccess(AuthenticationFlowContext context, String username) {
        LOG.infof("[NovaPulse 2FA AUDIT] SUCCESS | realm=%s | user=%s | ip=%s",
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
        // User is already set by the preceding 1FA step
        return true;
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
