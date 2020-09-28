package nl.uu.cs.iss.ga.sim2apl.core.deliberation;

import java.util.List;

/**
 * A deliberation step furthers the state of an agent. The DeliberationActionStep is a
 * special type of deliberation step that is allowed to produce actions. This type of
 * deliberation step may only be used in the act part of the sense-reason-act (deliberation)
 * cycle, and can be used in simulations where agents do not have direct access to the environment
 * but rather communicate their intentions to the environment.
 *
 * @author Bas Testerink
 */
public interface DeliberationActionStep<T> {

    /** Execution of this deliberation step. It is intended that steps are
     * designed in a modular fashion. For instance the default deliberation steps
     * each implement a single step from the 2APL deliberation cycle. This way
     * an agent component factory can decide at runtime which deliberation steps
     * should be part of an agents' deliberation cycle.
     **/
    List<T> execute() throws DeliberationStepException;
}
