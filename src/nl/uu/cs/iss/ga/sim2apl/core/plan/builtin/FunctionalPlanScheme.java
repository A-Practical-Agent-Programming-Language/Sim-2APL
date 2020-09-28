package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentContextInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme;

/**
 * A premade plan scheme to make code more concise when developing an agent. 
 * @author Bas Testerink
 */
public final class FunctionalPlanScheme<T> implements PlanScheme<T> {
	private final FunctionalPlanSchemeInterface<T> myInterface;
	
	public FunctionalPlanScheme(final FunctionalPlanSchemeInterface<T> myInterface){
		this.myInterface = myInterface;
	}
	
	@Override
	public final Plan<T> instantiate(final Trigger trigger, final AgentContextInterface<T> contextInterface){
		SubPlanInterface<T> plan = this.myInterface.getPlan(trigger, contextInterface);
		if(plan.equals(SubPlanInterface.UNINSTANTIATED())) return Plan.UNINSTANTIATED();
		else return new RunOncePlan<>() {
			@Override
			public final T executeOnce(final PlanToAgentInterface<T> planInterface)
					throws PlanExecutionError {
				return plan.execute(planInterface);
			}
		};
	}

}
