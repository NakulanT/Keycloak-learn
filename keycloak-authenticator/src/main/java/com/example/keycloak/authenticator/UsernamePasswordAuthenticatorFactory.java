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
 * Factory that registers {@link UsernamePasswordAuthenticator} with Keycloak.
 *
 * <p>
 * Provider ID: {@value #PROVIDER_ID}
 * <br>
 * Place this as the <strong>first REQUIRED step</strong> of your flow, followed
 * by {@link EmailOtpAuthenticatorFactory}.
 */
public class UsernamePasswordAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "novapulse-username-password";

    private static final UsernamePasswordAuthenticator SINGLETON = new UsernamePasswordAuthenticator();

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
        return "NovaPulse: Username + Password (1FA)";
    }

    @Override
    public String getHelpText() {
        return "Validates username and password. On success, generates a 6-digit OTP, "
                + "emails it to the user, and advances to the next step (Email OTP 2FA). "
                + "Includes brute-force awareness, blank-field guards, and audit logging.";
    }

    @Override
    public String getReferenceCategory() {
        return "password";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
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
