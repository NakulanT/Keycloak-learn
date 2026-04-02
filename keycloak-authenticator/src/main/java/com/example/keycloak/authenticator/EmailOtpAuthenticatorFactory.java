package com.example.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory that registers {@link EmailOtpAuthenticator} with Keycloak.
 *
 * <p>
 * Provider ID: {@value #PROVIDER_ID}
 * <br>
 * Place this as the <strong>second REQUIRED step</strong> of your flow,
 * immediately after {@link UsernamePasswordAuthenticatorFactory}.
 */
public class EmailOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "novapulse-email-otp";

    /**
     * Config key for the OTP validity window (seconds). Defaults to {@code 300}
     * (5 minutes). Exposed in the Keycloak Admin UI when configuring the flow.
     */
    public static final String CFG_OTP_TTL = "otpTtlSeconds";

    private static final EmailOtpAuthenticator SINGLETON = new EmailOtpAuthenticator();

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
        return "NovaPulse: Email OTP (2FA)";
    }

    @Override
    public String getHelpText() {
        return "Presents a 6-digit OTP input form and verifies the code that was emailed "
                + "by the preceding Username + Password (1FA) step. Supports expiry "
                + "detection and single-use enforcement.";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
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
