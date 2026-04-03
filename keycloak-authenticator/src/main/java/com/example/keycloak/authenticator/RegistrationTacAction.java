package com.example.keycloak.authenticator;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;

import java.util.List;

/**
 * NovaPulse — Terms &amp; Conditions during Registration (FormAction).
 *
 * <p>
 * This FormAction runs as the <strong>last step</strong> inside the Keycloak
 * Registration form flow, immediately after {@link RegistrationOtpAction} has
 * verified the email OTP.
 *
 * <p>
 * Works in two sub-steps tracked by the auth-session note {@value #NOTE_STEP}:
 * <ol>
 * <li><b>First pass (SHOW)</b> – on every page render until the user has
 * submitted a decision, {@link #buildPage} adds the {@code tacStep =
 *       "SHOW"} attribute so that {@code register.ftl} (or a combined form)
 * displays the T&amp;C block.</li>
 * <li><b>On POST (DECIDE)</b> – {@link #validate} reads the {@code decision}
 * field: {@code accept} → success; anything else → error (re-render
 * the T&amp;C page).</li>
 * </ol>
 *
 * <p>
 * Unlike the RequiredActionProvider approach this FormAction fires
 * <em>before</em> the user account is persisted, so a "decline" never
 * creates any user record.
 */
public class RegistrationTacAction implements FormAction {

    private static final Logger LOG = Logger.getLogger(RegistrationTacAction.class);

    /** Auth-session note tracking the T&C sub-step. */
    static final String NOTE_STEP = "novapulse_tac_step";
    static final String STEP_SHOW = "SHOW";
    static final String STEP_DECIDED = "DECIDED";

    /** Form field set by the T&C page Accept / Decline buttons. */
    static final String FIELD_DECISION = "tacDecision";
    static final String DECISION_ACCEPT = "accept";

    // ─────────────────────────────────────────────────────────────────────────
    // FormAction interface
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by Keycloak to add extra attributes before the page renders.
     * We set {@code tacStep = "SHOW"} so that the registration form (or a
     * dedicated T&C template) can conditionally display the T&C block.
     */
    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        String step = context.getAuthenticationSession().getAuthNote(NOTE_STEP);
        if (!STEP_DECIDED.equals(step)) {
            // Haven't accepted yet — show T&C block
            form.setAttribute("tacStep", STEP_SHOW);
        }
    }

    /**
     * Processes every POST. Checks whether the T&C decision has been made.
     */
    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        String decision = form.getFirst(FIELD_DECISION);

        // If DECIDED already (from a previous POST) just pass through
        if (STEP_DECIDED.equals(
                context.getAuthenticationSession().getAuthNote(NOTE_STEP))) {
            context.success();
            return;
        }

        // No decision field present yet → show T&C (re-render with tac block)
        if (decision == null || decision.isBlank()) {
            // Set step to SHOW so buildPage renders the T&C
            context.getAuthenticationSession().setAuthNote(NOTE_STEP, STEP_SHOW);
            // Trigger re-render with T&C block visible (empty validation error)
            context.validationError(form, List.of());
            return;
        }

        if (DECISION_ACCEPT.equalsIgnoreCase(decision)) {
            context.getAuthenticationSession().setAuthNote(NOTE_STEP, STEP_DECIDED);
            LOG.infof("[NovaPulse Reg T&C] User accepted T&C");
            context.success();
        } else {
            LOG.infof("[NovaPulse Reg T&C] User declined T&C — blocking registration");
            context.error(Errors.CONSENT_DENIED);
            context.validationError(form, List.of(
                    new FormMessage(FIELD_DECISION, "tacDeclined")));
        }
    }

    /** Clean up our notes once account creation succeeds. */
    @Override
    public void success(FormContext context) {
        context.getAuthenticationSession().removeAuthNote(NOTE_STEP);
        LOG.infof("[NovaPulse Reg T&C] Account created — T&C notes cleared");
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
}
