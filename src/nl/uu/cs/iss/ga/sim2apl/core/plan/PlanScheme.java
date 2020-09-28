package nl.uu.cs.iss.ga.sim2apl.core.plan;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentContextInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;

/**
 * A plan scheme specifies when a certain plan is relevant and applicable given 
 * a trigger and the context container of an agent. 
 * 
 * @author Bas Testerink
 */
public interface PlanScheme<T> {
	/**
	 * Try to instantiate the plan scheme. Must return Plan.uninstantiated() if the plan scheme is not 
	 * relevant or applicable for the given trigger and context. If the return value is
	 * not null then it will be adopted as a current plan and can be executed by a deliberation
	 * step. 
	 * @param trigger Trigger that must be processed.
	 * @param contextInterface An interface to obtain the context of the agent.
	 * @return Plan.uninstantiated() iff the plan scheme is not relevant and applicable, otherwise the plan to be scheduled for execution in the current deliberation cycle.
	 */
	Plan<T> instantiate(final Trigger trigger, final AgentContextInterface<T> contextInterface);
}