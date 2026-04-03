package com.example.keycloak.authenticator;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.*;

/**
 * NovaPulse — Terms &amp; Conditions Required Action.
 *
 * <p>
 * On the <em>first sign-in</em> (detected by the absence of the user attribute
 * {@value #ATTR_TC_ACCEPTED}) Keycloak redirects the user to a T&amp;C
 * acceptance
 * page ({@code terms-and-conditions.ftl}).
 * <ul>
 * <li><b>Accept</b> → sets {@value #ATTR_TC_ACCEPTED} = {@code "true"}, calls
 * {@code context.success()} and lets the user proceed.</li>
 * <li><b>Decline</b> → calls {@code context.failure()}, aborting the
 * session.</li>
 * </ul>
 *
 * <p>
 * Register this as a <strong>Default Action</strong> in the Admin Console so it
 * is automatically assigned to new users.
 */
public class TermsAndConditionsAction implements RequiredActionProvider {

    private static final Logger LOG = Logger.getLogger(TermsAndConditionsAction.class);

    /** User attribute that records T&C acceptance. */
    static final String ATTR_TC_ACCEPTED = "novapulse_tc_accepted";

    /** Form parameter sent by the T&C page buttons. */
    static final String PARAM_DECISION = "decision";
    static final String DECISION_ACCEPT = "accept";
    static final String DECISION_DECLINE = "decline";

    // ─────────────────────────────────────────────────────────────────────────
    // RequiredActionProvider interface
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renders the Terms &amp; Conditions page if the user hasn't accepted yet.
     */
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        LOG.infof("[NovaPulse T&C] Showing T&C page for user: %s",
                context.getUser().getUsername());
        Response challenge = context.form()
                .setAttribute("realm", context.getRealm())
                .createForm("terms-and-conditions.ftl");
        context.challenge(challenge);
    }

    /**
     * Processes the form POST from the T&C page.
     */
    @Override
    public void processAction(RequiredActionContext context) {
        String decision = context.getHttpRequest().getDecodedFormParameters()
                .getFirst(PARAM_DECISION);

        if (DECISION_ACCEPT.equalsIgnoreCase(decision)) {
            UserModel user = context.getUser();
            user.setSingleAttribute(ATTR_TC_ACCEPTED, "true");
            user.removeRequiredAction(TermsAndConditionsActionFactory.PROVIDER_ID);
            LOG.infof("[NovaPulse T&C] User %s accepted T&C", user.getUsername());
            context.success();
        } else {
            LOG.infof("[NovaPulse T&C] User %s declined T&C — aborting session",
                    context.getUser().getUsername());
            context.failure();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Required stubs
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // Trigger this action for users who haven't accepted T&C yet
        UserModel user = context.getUser();
        if (user == null)
            return;

        String accepted = user.getFirstAttribute(ATTR_TC_ACCEPTED);
        if (!"true".equalsIgnoreCase(accepted)) {
            user.addRequiredAction(TermsAndConditionsActionFactory.PROVIDER_ID);
        }
    }

    @Override
    public void close() {
    }
}
