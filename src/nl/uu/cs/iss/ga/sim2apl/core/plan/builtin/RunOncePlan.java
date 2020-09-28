package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;

/**
 * A plan that is automatically set to finished after it has been executed. 
 * 
 * @author Bas Testerink
 */
public abstract class RunOncePlan<T> extends Plan<T> {
 
	@Override
	public final T execute(final PlanToAgentInterface<T> planInterface) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {
		T planAction = executeOnce(planInterface);
		setFinished(true);
		return planAction;
	}
	
	public abstract T executeOnce(final PlanToAgentInterface<T> planInterface) throws PlanExecutionError;
}