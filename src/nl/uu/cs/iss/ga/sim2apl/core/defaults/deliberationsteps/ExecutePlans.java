package nl.uu.cs.iss.ga.sim2apl.core.defaults.deliberationsteps;


import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationActionStep;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationStepException;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberation step for executing the current plans of the agent.
 * @author Bas Testerink
 */
public final class ExecutePlans<T> implements DeliberationActionStep<T> {
	private final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent<T> agent;

	public ExecutePlans(final Agent<T> agent){
		this.agent = agent;
	}
	
	/** This steps executes by going through each of the agent's plans. If the plan is finished 
	 * after its execution, then it is removed. If an error occurs, then a plan execution error
	 * will be inserted as an internal trigger. */
	@Override
	public final List<T> execute() throws DeliberationStepException {
		ArrayList<T> producedActionList = new ArrayList<>();
		for(Plan<T> plan : this.agent.getPlans()){
			try {
				T planAction = this.agent.executePlan(plan);
				if(planAction != null)
					producedActionList.add(planAction);
				if(plan.isFinished())
					this.agent.removePlan(plan);
			} catch(PlanExecutionError executionError){
				// NOTE: if a plan has an execution error, and a goal is being pursued by the plan, then the goal still is 
				// flagged as being pursued. Therefore it is important to ALWAYS have repair plan schemes for failed goal plan schemes.

				Class c;
				try {
					c = Class.forName(executionError.getStackTrace()[0].getClassName());
				} catch(Exception e) {
					c = getClass();
				}

				this.agent.removePlan(plan); // Remove plan from execution
				this.agent.addInternalTrigger(executionError); // Add the error
			}
		}
		return producedActionList;
	}
}
