package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentContextInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
/**
 *	An interface for creating FunctionalPlanSchemes. 
 *  @author Bas Testerink
 */
public interface FunctionalPlanSchemeInterface<T> {
	/** Get the plan for a given trigger and context interface. Return SubPlanInterface.UNINSTANTIATED if the plan did not fire. */
	public SubPlanInterface<T> getPlan(Trigger trigger, AgentContextInterface contextInterface);
}
