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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
public final class OtpUtils {

    private static final Logger LOG = Logger.getLogger(OtpUtils.class);

    // ── Auth-session note keys ────────────────────────────────────────────────
    static final String NOTE_OTP_VALUE = "novapulse_otp_value";
    static final String NOTE_OTP_EXPIRY = "novapulse_otp_expiry";

    /** OTP validity window in seconds (5 minutes). */
    static final long OTP_TTL_SECONDS = 300L;

    /** Digits in the OTP code. */
    private static final int OTP_DIGITS = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtils() {
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
     * Sends an OTP email to a raw email address (used during registration
     * before the user model exists). Wraps the address in a minimal
     * {@link UserModel} adapter so we can reuse {@link EmailSenderProvider}.
     */
    public static void sendRegistrationOtpEmail(KeycloakSession session,
            RealmModel realm,
            String toEmail,
            String otp) {
        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        long ttlMinutes = OTP_TTL_SECONDS / 60;

        String subject = "[" + realmName + "] Verify your email to complete registration";

        String textBody = realmName + " — Email verification code\n"
                + "=========================================\n\n"
                + "Your registration verification code is:\n\n"
                + "  " + otp + "\n\n"
                + "This code expires in " + ttlMinutes + " minutes.\n\n"
                + "If you did not request this code, please ignore this email.\n\n"
                + "--\n"
                + realmName + " — automated security message";

        String htmlBody = "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;\">"
                + "<h2>" + realmName + " — Email verification code</h2>"
                + "<p>Your registration verification code is:</p>"
                + "<h1 style=\"letter-spacing:6px;color:#1a73e8;\">" + otp + "</h1>"
                + "<p>This code expires in <strong>" + ttlMinutes + " minutes</strong>.</p>"
                + "<hr/>"
                + "<p style=\"color:#888;font-size:12px;\">If you did not register for an account, "
                + "please ignore this email.</p>"
                + "</body></html>";

        try {
            UserModel recipient = new SimpleEmailUser(toEmail);
            Map<String, String> smtpConfig = realm.getSmtpConfig();
            session.getProvider(EmailSenderProvider.class)
                    .send(smtpConfig, recipient, subject, textBody, htmlBody);
            LOG.infof("[NovaPulse OTP] Registration OTP email dispatched to %s", toEmail);
        } catch (EmailException e) {
            LOG.errorf(e, "[NovaPulse OTP] Failed to send registration OTP email to %s", toEmail);
            throw new RuntimeException("Failed to send registration OTP email", e);
        }
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
    // Minimal UserModel adapter — used only for sending emails to an address
    // that doesn't yet have a persisted user (e.g. during registration).
    // ─────────────────────────────────────────────────────────────────────────

    private static final class SimpleEmailUser implements UserModel {
        private final String email;

        SimpleEmailUser(String email) {
            this.email = email;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getFirstName() {
            return "";
        }

        @Override
        public String getLastName() {
            return "";
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public void setUsername(String username) {
        }

        @Override
        public Long getCreatedTimestamp() {
            return null;
        }

        @Override
        public void setCreatedTimestamp(Long timestamp) {
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public void setSingleAttribute(String name, String value) {
        }

        @Override
        public void setAttribute(String name, List<String> values) {
        }

        @Override
        public void removeAttribute(String name) {
        }

        @Override
        public String getFirstAttribute(String name) {
            return null;
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            return Stream.empty();
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Stream<String> getRequiredActionsStream() {
            return Stream.empty();
        }

        @Override
        public void addRequiredAction(String action) {
        }

        @Override
        public void removeRequiredAction(String action) {
        }

        @Override
        public void setFirstName(String firstName) {
        }

        @Override
        public void setLastName(String lastName) {
        }

        @Override
        public boolean isEmailVerified() {
            return true;
        }

        @Override
        public void setEmailVerified(boolean verified) {
        }

        @Override
        public void setEmail(String email) {
        }

        @Override
        public Stream<org.keycloak.models.GroupModel> getGroupsStream() {
            return Stream.empty();
        }

        @Override
        public void joinGroup(org.keycloak.models.GroupModel group) {
        }

        @Override
        public void leaveGroup(org.keycloak.models.GroupModel group) {
        }

        @Override
        public boolean isMemberOf(org.keycloak.models.GroupModel group) {
            return false;
        }

        @Override
        public String getFederationLink() {
            return null;
        }

        @Override
        public void setFederationLink(String link) {
        }

        @Override
        public String getServiceAccountClientLink() {
            return null;
        }

        @Override
        public void setServiceAccountClientLink(String clientInternalId) {
        }

        @Override
        public org.keycloak.models.SubjectCredentialManager credentialManager() {
            return null;
        }

        @Override
        public Stream<org.keycloak.models.RoleModel> getRealmRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public Stream<org.keycloak.models.RoleModel> getClientRoleMappingsStream(org.keycloak.models.ClientModel app) {
            return Stream.empty();
        }

        @Override
        public boolean hasRole(org.keycloak.models.RoleModel role) {
            return false;
        }

        @Override
        public void grantRole(org.keycloak.models.RoleModel role) {
        }

        @Override
        public Stream<org.keycloak.models.RoleModel> getRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public void deleteRoleMapping(org.keycloak.models.RoleModel role) {
        }
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
