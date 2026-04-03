package com.example.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory that registers {@link TermsAndConditionsAction} with Keycloak.
 *
 * <p>
 * Provider ID: {@value #PROVIDER_ID}
 * <br>
 * Enable this in Admin Console → Authentication → Required Actions, then tick
 * <em>Set as default action</em> so new users automatically see the T&amp;C
 * page
 * on first login.
 */
public class TermsAndConditionsActionFactory implements RequiredActionFactory {

    public static final String PROVIDER_ID = "novapulse-terms-and-conditions";

    private static final TermsAndConditionsAction SINGLETON = new TermsAndConditionsAction();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "NovaPulse: Terms and Conditions Acceptance";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
