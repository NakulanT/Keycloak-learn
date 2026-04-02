package com.example.keycloak.authenticator.utils;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

/**
 * Stateless helper for generating, storing, and validating email OTPs.
 *
 * <p>
 * OTP state is kept exclusively in Keycloak auth-session notes — no
 * external store needed, and the state is automatically discarded when
 * the session expires.
 *
 * <p>
 * Auth-session note keys used:
 * <ul>
 * <li>{@value #NOTE_OTP_VALUE} — the 6-digit code (plain text)</li>
 * <li>{@value #NOTE_OTP_EXPIRY} — epoch-second expiry timestamp</li>
 * </ul>
 */
public final class AuthUtils {

    private static final Logger LOG = Logger.getLogger(AuthUtils.class);

    // ── Auth-session note keys ────────────────────────────────────────────────
    static final String NOTE_OTP_VALUE = "novapulse_otp_value";
    static final String NOTE_OTP_EXPIRY = "novapulse_otp_expiry";

    /** OTP validity window in seconds (5 minutes). */
    static final long OTP_TTL_SECONDS = 300L;

    /** Digits in the OTP code. */
    private static final int OTP_DIGITS = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AuthUtils() {
        /* utility class */ }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a new OTP, persists it (with expiry) into the auth session,
     * and returns the plain-text code for sending.
     */
    public static String generateAndStore(AuthenticationFlowContext context) {
        String otp = generateOtp();
        long expiry = Instant.now().getEpochSecond() + OTP_TTL_SECONDS;

        context.getAuthenticationSession().setAuthNote(NOTE_OTP_VALUE, otp);
        context.getAuthenticationSession().setAuthNote(NOTE_OTP_EXPIRY, String.valueOf(expiry));

        LOG.debugf("[NovaPulse OTP] Generated OTP for session %s, expires at epoch %d",
                context.getAuthenticationSession().getParentSession().getId(), expiry);

        return otp;
    }

    /**
     * Validates the submitted code against the stored OTP.
     *
     * <p>
     * On success the stored notes are cleared (single-use guarantee).
     */
    public static ValidationResult validate(AuthenticationFlowContext context, String submitted) {
        String stored = context.getAuthenticationSession().getAuthNote(NOTE_OTP_VALUE);
        String expiryStr = context.getAuthenticationSession().getAuthNote(NOTE_OTP_EXPIRY);

        if (stored == null || expiryStr == null) {
            LOG.warn("[NovaPulse OTP] No OTP found in session — treating as invalid");
            return ValidationResult.INVALID;
        }

        long expiry;
        try {
            expiry = Long.parseLong(expiryStr);
        } catch (NumberFormatException e) {
            LOG.warn("[NovaPulse OTP] Malformed expiry note — treating as invalid");
            return ValidationResult.INVALID;
        }

        if (Instant.now().getEpochSecond() > expiry) {
            clearOtpNotes(context);
            return ValidationResult.EXPIRED;
        }

        if (!stored.equals(submitted.trim())) {
            return ValidationResult.INVALID;
        }

        // Single-use: clear after successful validation
        clearOtpNotes(context);
        return ValidationResult.VALID;
    }

    /**
     * Sends the OTP to the user's email via Keycloak's {@link EmailSenderProvider},
     * using inline HTML and plain-text bodies (no .ftl template files required).
     */
    public static void sendOtpEmail(KeycloakSession session,
            RealmModel realm,
            UserModel user,
            String otp) {
        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        long ttlMinutes = OTP_TTL_SECONDS / 60;

        String subject = "[" + realmName + "] Your sign-in verification code";

        String textBody = realmName + " — Sign-in verification code\n"
                + "=========================================\n\n"
                + "Your one-time sign-in code is:\n\n"
                + "  " + otp + "\n\n"
                + "This code expires in " + ttlMinutes + " minutes.\n\n"
                + "If you did not request this code, please ignore this email.\n"
                + "Your account remains secure.\n\n"
                + "--\n"
                + realmName + " — automated security message";

        String htmlBody = "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;\">"
                + "<h2>" + realmName + " — Sign-in verification code</h2>"
                + "<p>Your one-time sign-in code is:</p>"
                + "<h1 style=\"letter-spacing:6px;color:#1a73e8;\">" + otp + "</h1>"
                + "<p>This code expires in <strong>" + ttlMinutes + " minutes</strong>.</p>"
                + "<hr/>"
                + "<p style=\"color:#888;font-size:12px;\">If you did not request this code, "
                + "please ignore this email. Your account remains secure.</p>"
                + "</body></html>";

        try {
            Map<String, String> smtpConfig = realm.getSmtpConfig();
            session.getProvider(EmailSenderProvider.class)
                    .send(smtpConfig, user, subject, textBody, htmlBody);

            LOG.infof("[NovaPulse OTP] OTP email dispatched to %s", user.getEmail());

        } catch (EmailException e) {
            LOG.errorf(e, "[NovaPulse OTP] Failed to send OTP email to %s", user.getEmail());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    private static String generateOtp() {
        int bound = (int) Math.pow(10, OTP_DIGITS); // 1_000_000 for 6 digits
        // Zero-pad to ensure fixed length (e.g. 000042)
        return String.format("%0" + OTP_DIGITS + "d", RANDOM.nextInt(bound));
    }

    private static void clearOtpNotes(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_VALUE);
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_EXPIRY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result enum
    // ─────────────────────────────────────────────────────────────────────────

    public enum ValidationResult {
        VALID,
        EXPIRED,
        INVALID
    }
}
