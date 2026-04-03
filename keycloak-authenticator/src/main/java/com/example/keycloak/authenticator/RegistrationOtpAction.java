package com.example.keycloak.authenticator;

import com.example.keycloak.authenticator.utils.OtpUtils;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

/**
 * NovaPulse — Registration OTP Verification (FormAction).
 *
 * <p>
 * Works in two sub-steps within the same Keycloak registration form action:
 * <ol>
 * <li><b>First POST</b> – standard registration form data arrives.
 * Basic field validation is performed, an OTP is generated and emailed
 * to the provided address, and the auth-session note is set to VERIFY.</li>
 * <li><b>Second POST</b> – the OTP form is submitted (via
 * {@code register-otp-verify.ftl}).
 * The code is verified; success lets the flow continue (account creation);
 * failure re-renders the OTP form with an error.</li>
 * </ol>
 *
 * <p>
 * The sub-step is tracked via the auth-session note {@value #NOTE_STEP}.
 */
public class RegistrationOtpAction implements FormAction {

    private static final Logger LOG = Logger.getLogger(RegistrationOtpAction.class);

    // ── Auth-session note keys ────────────────────────────────────────────────
    static final String NOTE_STEP = "novapulse_reg_step";
    static final String STEP_COLLECT = "COLLECT";
    static final String STEP_VERIFY = "VERIFY";

    /** Cached email while the user is on the OTP page. */
    static final String NOTE_EMAIL = "novapulse_reg_email";

    // ── OTP state keys (registration-specific) ─────────────────────────────
    static final String NOTE_OTP_VALUE = "novapulse_reg_otp_value";
    static final String NOTE_OTP_EXPIRY = "novapulse_reg_otp_expiry";

    // ── Form field names ──────────────────────────────────────────────────────
    static final String FIELD_OTP = "otp";
    static final String FIELD_EMAIL = "email";

    private static final long OTP_TTL = 300L; // 5 minutes
    private static final int OTP_DIGITS = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // FormAction interface
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by Keycloak to add extra content to the registration form page.
     * When in VERIFY sub-step, we render the OTP verification page instead.
     */
    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        String step = context.getAuthenticationSession().getAuthNote(NOTE_STEP);
        if (STEP_VERIFY.equals(step)) {
            String maskedEmail = maskEmail(
                    context.getAuthenticationSession().getAuthNote(NOTE_EMAIL));
            form.setAttribute("maskedEmail", maskedEmail);
            form.setAttribute("regOtpStep", "VERIFY");
        }
    }

    /**
     * Validate is called on every POST. We use it to handle both sub-steps:
     * 1. Collecting the registration form → send OTP.
     * 2. Verifying the OTP submitted by the user.
     */
    @Override
    public void validate(ValidationContext context) {
        String step = context.getAuthenticationSession().getAuthNote(NOTE_STEP);
        if (STEP_VERIFY.equals(step)) {
            validateOtp(context);
        } else {
            collectAndSendOtp(context);
        }
    }

    /** Called after all form actions succeed — clean up our temp notes. */
    @Override
    public void success(FormContext context) {
        context.getAuthenticationSession().removeAuthNote(NOTE_STEP);
        context.getAuthenticationSession().removeAuthNote(NOTE_EMAIL);
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_VALUE);
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_EXPIRY);
        LOG.infof("[NovaPulse Reg OTP] Account created successfully — notes cleared");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-step 1: Send OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void collectAndSendOtp(ValidationContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        String email = form.getFirst(FIELD_EMAIL);

        if (isBlank(email)) {
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(form, List.of(new FormMessage(FIELD_EMAIL, "missingEmail")));
            return;
        }

        // Generate and store OTP
        String otp = generateOtp();
        long expiry = Instant.now().getEpochSecond() + OTP_TTL;

        context.getAuthenticationSession().setAuthNote(NOTE_OTP_VALUE, otp);
        context.getAuthenticationSession().setAuthNote(NOTE_OTP_EXPIRY, String.valueOf(expiry));
        context.getAuthenticationSession().setAuthNote(NOTE_EMAIL, email);
        context.getAuthenticationSession().setAuthNote(NOTE_STEP, STEP_VERIFY);

        // Send OTP email
        try {
            OtpUtils.sendRegistrationOtpEmail(context.getSession(), context.getRealm(), email, otp);
        } catch (RuntimeException e) {
            LOG.errorf(e, "[NovaPulse Reg OTP] Failed to send OTP to %s", email);
            context.getAuthenticationSession().removeAuthNote(NOTE_STEP);
            context.error(Errors.EMAIL_SEND_FAILED);
            context.validationError(form, List.of(new FormMessage("emailSendFailed")));
            return;
        }

        LOG.infof("[NovaPulse Reg OTP] OTP sent to %s — transitioning to VERIFY", email);
        // Mark as error so Keycloak re-renders the page (which will show
        // register-otp-verify.ftl)
        context.validationError(form, List.of());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-step 2: Verify OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void validateOtp(ValidationContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        String submitted = form.getFirst(FIELD_OTP);

        if (isBlank(submitted)) {
            context.error(Errors.INVALID_USER_CREDENTIALS);
            context.validationError(form, List.of(new FormMessage(FIELD_OTP, "missingOtp")));
            return;
        }

        String stored = context.getAuthenticationSession().getAuthNote(NOTE_OTP_VALUE);
        String expiryS = context.getAuthenticationSession().getAuthNote(NOTE_OTP_EXPIRY);

        if (stored == null || expiryS == null) {
            LOG.warn("[NovaPulse Reg OTP] OTP notes missing");
            context.error(Errors.INVALID_USER_CREDENTIALS);
            context.validationError(form, List.of(new FormMessage(FIELD_OTP, "invalidOtp")));
            return;
        }

        long expiry;
        try {
            expiry = Long.parseLong(expiryS);
        } catch (NumberFormatException e) {
            expiry = 0;
        }

        if (Instant.now().getEpochSecond() > expiry) {
            LOG.infof("[NovaPulse Reg OTP] OTP expired");
            clearOtpNotes(context);
            context.error(Errors.EXPIRED_CODE);
            context.validationError(form, List.of(new FormMessage(FIELD_OTP, "otpExpired")));
            return;
        }

        if (!stored.equals(submitted.trim())) {
            LOG.infof("[NovaPulse Reg OTP] OTP mismatch");
            context.error(Errors.INVALID_USER_CREDENTIALS);
            context.validationError(form, List.of(new FormMessage(FIELD_OTP, "invalidOtp")));
            return;
        }

        // ✓ Valid — clear and allow account creation
        clearOtpNotes(context);
        context.getAuthenticationSession().removeAuthNote(NOTE_STEP);
        LOG.infof("[NovaPulse Reg OTP] OTP verified — proceeding to account creation");
        context.success();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Required stubs
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

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_DIGITS);
        return String.format("%0" + OTP_DIGITS + "d", RANDOM.nextInt(bound));
    }

    private void clearOtpNotes(ValidationContext context) {
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_VALUE);
        context.getAuthenticationSession().removeAuthNote(NOTE_OTP_EXPIRY);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
}
