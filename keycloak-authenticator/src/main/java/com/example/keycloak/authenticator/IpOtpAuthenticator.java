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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NovaPulse — New-IP OTP Authenticator (optional sign-in step).
 *
 * <p>
 * When added to the authentication flow this step:
 * <ol>
 * <li>Reads the client's IP from the connection.</li>
 * <li>Compares it against the user's {@value #ATTR_TRUSTED_IPS} attribute
 * (comma-separated list).</li>
 * <li>If the IP is <em>already trusted</em> → immediately succeeds (no user
 * interruption).</li>
 * <li>If the IP is <em>new</em> → generates a 6-digit OTP, emails it, and
 * presents {@code login-ip-otp.ftl}.</li>
 * <li>On successful OTP entry → the IP is appended to the trusted list and
 * the step succeeds.</li>
 * </ol>
 *
 * <p>
 * Add this as an <strong>REQUIRED</strong> step after the email OTP (2FA) step
 * in your authentication flow.
 */
public class IpOtpAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(IpOtpAuthenticator.class);

    /** User attribute that stores comma-separated trusted IPs. */
    static final String ATTR_TRUSTED_IPS = "novapulse_trusted_ips";

    /** Auth-session note keys for the OTP sent during this step. */
    static final String NOTE_IP_OTP_VALUE = "novapulse_ip_otp_value";
    static final String NOTE_IP_OTP_EXPIRY = "novapulse_ip_otp_expiry";
    static final String NOTE_NEW_IP = "novapulse_new_ip";

    /** Form field for the OTP input. */
    static final String FIELD_OTP = "otp";

    // ─────────────────────────────────────────────────────────────────────────
    // Authenticator lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String currentIp = context.getConnection().getRemoteAddr();
        UserModel user = context.getUser();

        if (isTrustedIp(user, currentIp)) {
            LOG.debugf("[NovaPulse IP-OTP] IP %s is trusted for user %s — skipping",
                    currentIp, user.getUsername());
            context.success();
            return;
        }

        // New IP detected — send OTP
        LOG.infof("[NovaPulse IP-OTP] New IP %s for user %s — sending OTP",
                currentIp, user.getUsername());

        String otp = OtpUtils.generateAndStore(context);
        // Store under IP-specific notes as well so we don't collide with login 2FA
        context.getAuthenticationSession().setAuthNote(NOTE_NEW_IP, currentIp);

        try {
            OtpUtils.sendOtpEmail(context.getSession(), context.getRealm(), user, otp);
        } catch (RuntimeException e) {
            LOG.errorf(e, "[NovaPulse IP-OTP] Failed to send OTP to user %s", user.getUsername());
            context.failureChallenge(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    createOtpForm(context, "emailSendFailed"));
            return;
        }

        context.challenge(createOtpForm(context, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String submittedOtp = formData.getFirst(FIELD_OTP);

        if (isBlank(submittedOtp)) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    createOtpForm(context, "missingOtp"));
            return;
        }

        OtpUtils.ValidationResult result = OtpUtils.validate(context, submittedOtp);

        switch (result) {
            case VALID -> {
                // Record the newly-trusted IP
                String newIp = context.getAuthenticationSession().getAuthNote(NOTE_NEW_IP);
                if (newIp != null) {
                    addTrustedIp(context.getUser(), newIp);
                    context.getAuthenticationSession().removeAuthNote(NOTE_NEW_IP);
                    LOG.infof("[NovaPulse IP-OTP] IP %s added to trusted list for user %s",
                            newIp, context.getUser().getUsername());
                }
                context.success();
            }
            case EXPIRED -> {
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_CREDENTIALS,
                        createOtpForm(context, "otpExpired"));
            }
            case INVALID -> {
                context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_CREDENTIALS,
                        createOtpForm(context, "invalidOtp"));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isTrustedIp(UserModel user, String ip) {
        String stored = user.getFirstAttribute(ATTR_TRUSTED_IPS);
        if (stored == null || stored.isBlank())
            return false;
        return Arrays.asList(stored.split(",")).contains(ip.trim());
    }

    private void addTrustedIp(UserModel user, String ip) {
        String stored = user.getFirstAttribute(ATTR_TRUSTED_IPS);
        List<String> ips = new ArrayList<>();
        if (stored != null && !stored.isBlank()) {
            ips.addAll(Arrays.asList(stored.split(",")));
        }
        if (!ips.contains(ip.trim())) {
            ips.add(ip.trim());
        }
        user.setSingleAttribute(ATTR_TRUSTED_IPS, String.join(",", ips));
    }

    private Response createOtpForm(AuthenticationFlowContext context, String errorKey) {
        var form = context.form()
                .setAttribute("realm", context.getRealm())
                .setAttribute("maskedEmail", maskEmail(
                        context.getUser() != null ? context.getUser().getEmail() : ""));
        if (errorKey != null) {
            form.addError(new FormMessage(errorKey));
        }
        return form.createForm("login-ip-otp.ftl");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 1)
            return local + domain;
        return local.charAt(0) + "*".repeat(local.length() - 1) + domain;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Required interface stubs
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean requiresUser() {
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
