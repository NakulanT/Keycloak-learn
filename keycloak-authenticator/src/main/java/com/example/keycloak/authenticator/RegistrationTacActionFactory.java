package com.example.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory that registers {@link RegistrationTacAction} with Keycloak.
 *
 * <p>
 * Provider ID: {@value #PROVIDER_ID}
 * <br>
 * Add this as a <strong>REQUIRED</strong> form action inside the
 * <em>Registration form</em> group, <em>after</em>
 * {@link RegistrationOtpActionFactory}.
 */
public class RegistrationTacActionFactory implements FormActionFactory {

    public static final String PROVIDER_ID = "novapulse-registration-tac";

    private static final RegistrationTacAction SINGLETON = new RegistrationTacAction();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "NovaPulse: Registration T&C Acceptance";
    }

    @Override
    public String getHelpText() {
        return "Shows Terms & Conditions during registration. "
                + "The user must accept before the account is created. "
                + "Add this AFTER the Email OTP step in the registration form flow.";
    }

    @Override
    public String getReferenceCategory() {
        return "terms";
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
    public FormAction create(KeycloakSession session) {
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
