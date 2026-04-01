package com.novapulse.keycloak.auth;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory that registers {@link CustomUsernamePasswordAuthenticator} with
 * Keycloak.
 *
 * <p>
 * The provider implements a single-step, two-stage flow:
 * <ol>
 * <li>Username + password validation.</li>
 * <li>Email OTP challenge (sent automatically after a valid password).</li>
 * </ol>
 */
public class CustomUsernamePasswordAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "novapulse-username-password-otp";

    // Configurable OTP TTL so admins can override the default without a
    // code change. The value is read at runtime in EmailOtpService if you
    // wire it through (see note in getConfigProperties).
    public static final String CFG_OTP_TTL = "otpTtlSeconds";

    private static final CustomUsernamePasswordAuthenticator SINGLETON = new CustomUsernamePasswordAuthenticator();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    // ─────────────────────────────────────────────────────────────────────────
    // AuthenticatorFactory
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "NovaPulse: Username + Password + Email OTP";
    }

    @Override
    public String getHelpText() {
        return "Two-stage authenticator: validates username/password, then sends a "
                + "6-digit OTP to the user's registered email address and verifies it. "
                + "Includes brute-force awareness, blank-field guards, and audit logging.";
    }

    @Override
    public String getReferenceCategory() {
        return "password";
    }

    @Override
    public boolean isConfigurable() {
        // Expose OTP TTL as an admin-configurable property
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty otpTtl = new ProviderConfigProperty(
                CFG_OTP_TTL,
                "OTP validity (seconds)",
                "How long the emailed OTP remains valid. Default: 300 (5 minutes).",
                ProviderConfigProperty.STRING_TYPE,
                "300");
        return List.of(otpTtl);
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void init(Config.Scope config) {
        /* no-op */ }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        /* no-op */ }

    @Override
    public void close() {
        /* no-op */ }
}