package nl.uu.cs.iss.ga.sim2apl.core.deliberation;

import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;
import nl.uu.cs.iss.ga.sim2apl.core.platform.Platform;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * A deliberation runnable implements how an agent is executed. This is done by 
 * grabbing the agent's deliberation cycle and execute each step. Then, if the 
 * agent is done, it will not reschedule itself, otherwise it will reschedule itself.
 *
 * @author Bas Testerink
 */
public final class DeliberationRunnable<T> implements Callable<ReschedulableResult<T>> {
	/** Interface to obtain the relevant agent's data. */
	private final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent<T> agent;
	/** Interface to the relevant platform functionalities. */
	private final Platform platform;

	private ArrayList<T> intendedActions;

	/**
	 * Creation of the deliberation runnable will also result in the setting of a self-rescheduler for this runnable  
	 * through the agent interface. 
	 * @param agent
	 * @param platform
	 */
	public DeliberationRunnable(final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent<T> agent, final Platform platform) {
		this.agent = agent;
		this.platform = platform;
		this.agent.setSelfRescheduler(new SelfRescheduler<>(this));
	}

	/**
	 * Run the deliberation cycle of the agent once. Will ask the platform
	 * to execute again sometime in the future if the agent is not done
	 * according to its <code>isDone</code> method. The agent is killed
	 * in case it is done, or if a <code>DeliberationStepException</code> occurs.
	 * If the agent is done or if a deliberation step exception occurs, then it will be
	 * killed and removed from the platform.
	 */
	@Override
	public ReschedulableResult<T> call(){
		if(!this.agent.isDone()){ // Check first if agent was killed outside of this runnable
            // Clear intended actions potential previous deliberation cycle
            this.intendedActions = new ArrayList<>();
			boolean reschedule = false;

			try {
				// Go through the cycle and execute each step.
				// Note that the deliberation cycle cannot change at runtime.  
				for(DeliberationStep step : this.agent.getSenseReasonCycle()){
					step.execute();
				}

				for(DeliberationActionStep<T> step : this.agent.getActCycle()) {
					this.intendedActions.addAll(step.execute());
				}

				// If all deliberation steps are finished, then check whether
				// the agent is done, so it can be killed.
				if(this.agent.isDone()){
					Platform.getLogger().log(DeliberationRunnable.class, String.format(
							"Agent %s is done and will be shut down",
							agent.getAID().getName()));
					initiateShutdown(this.agent);
				} else {
					if (!this.agent.checkSleeping()) { // If the agents goes to sleep then it will be woken upon any external input (message, external trigger)
//						reschedule();
						reschedule = true;
					} else {
						Platform.getLogger().log(DeliberationRunnable.class, String.format("Agent %s going to sleep",
								agent.getAID().getName()));
					}
				}
			} catch(DeliberationStepException exception){
				// Deliberation exceptions should not occur. The agent is 
				// killed and removed from the platform. All proxy's are
				// notified of the agent's death. The rest of the multi-
				// agent system will continue execution by default.
				Platform.getLogger().log(getClass(), exception);
				this.platform.killAgent(this.agent.getAID());
			}

            // Produce the set of intended actions
            return new ReschedulableResult<T>(this, intendedActions, reschedule);
		} else {
			initiateShutdown(agent);

			// An agent that shuts down will no longer perform actions
//            return Collections.emptyList();
			return new ReschedulableResult<>(this, intendedActions, false);
		}
	}

	/** Perform shutdown plans, and kill agent **/
	private void initiateShutdown(Agent<T> agent) {
		agent.getShutdownPlans().forEach(
				plan -> {
					try {
						agent.executePlan(plan);
					} catch (PlanExecutionError ex) {
						Platform.getLogger().log(plan.getClass(), ex);
						/*TODO?*/
					}
				}
		);
		this.platform.killAgent(agent.getAID());
	}

	/** Returns the id of the agent to which this runnable belongs. */
	public final AgentID getAgentID(){ return this.agent.getAID(); }
	
	/** Reschedule this deliberation runnable so it will be executed again in the future. */
	public final void reschedule(){
		this.platform.scheduleForExecution(this);
	}
}