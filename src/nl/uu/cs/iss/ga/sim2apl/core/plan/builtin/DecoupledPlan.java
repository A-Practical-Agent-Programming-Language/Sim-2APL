package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;

/**
 * A decoupled plan is a plan where the execute method contains a trigger. The standard use of this class is that the trigger which caused the 
 * plan scheme that instantiated the plan is the trigger which is provided in the execute method at runtime. 
 * 
 * @author Bas Testerink
 */
public abstract class DecoupledPlan<T> extends Plan<T> {
	/** {@inheritDoc} */
	@Override
	public final T execute(final PlanToAgentInterface<T> planInterface) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {return null;}
	public abstract T execute(final Trigger trigger, final PlanToAgentInterface<T> planInterface) throws PlanExecutionError;
}
