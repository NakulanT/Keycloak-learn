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
 * Factory that registers {@link IpOtpAuthenticator} with Keycloak.
 *
 * <p>
 * Provider ID: {@value #PROVIDER_ID}
 * <br>
 * Add this as a <strong>REQUIRED</strong> step after the Email OTP (2FA) step
 * in your authentication flow.
 */
public class IpOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "novapulse-ip-otp";

    private static final IpOtpAuthenticator SINGLETON = new IpOtpAuthenticator();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "NovaPulse: New-IP OTP Challenge";
    }

    @Override
    public String getHelpText() {
        return "Detects logins from new IP addresses and requires the user to verify "
                + "their identity via a 6-digit OTP emailed to them. "
                + "Trusted IPs are persisted as a user attribute.";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
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
