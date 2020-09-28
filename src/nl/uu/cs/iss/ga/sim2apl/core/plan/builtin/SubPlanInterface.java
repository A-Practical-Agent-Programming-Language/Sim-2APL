package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;

/**
 * This functional interface is used to create subplans that are executed across multiple deliberation cycles. 
 * 
 * @author Bas Testerink
 */
public interface SubPlanInterface<T> {
	/** Specification of the plan to be executed. */
	public T execute(final PlanToAgentInterface<T> planInterface) throws PlanExecutionError;
	/** A token that indicates that whatever scheme tries to make an interface did not fire. */ 
//	public final static SubPlanInterface<? extends Object> UNINSTANTIATED = new SubPlanInterface(){@Override
//	public final T execute(final PlanToAgentInterface<T> planInterface){return null;}};
	public static <T> SubPlanInterface<T> UNINSTANTIATED() {
		return new SubPlanInterface<T>() {
			@Override
			public T execute(PlanToAgentInterface<T> planInterface) throws PlanExecutionError {
				return null;
			}
		};
	}
}
