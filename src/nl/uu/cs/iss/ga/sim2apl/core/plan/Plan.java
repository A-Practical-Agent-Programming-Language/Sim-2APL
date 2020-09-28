package nl.uu.cs.iss.ga.sim2apl.core.plan;

import nl.uu.cs.iss.ga.sim2apl.core.agent.Goal;
import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
/**
 * Plans implement the decision making business logic of agents. Each plan is
 * created by a plan scheme. The plan scheme determines when a plan is to be applied.
 * If the plan was created to achieve a specific goal, then it will be canceled once 
 * the goal is achieved.
 * 
 * @author Bas Testerink
 */
public abstract class Plan<T> {
	/** Whether the plan is finished executing. */
	private boolean finished;
	/** Optionally the goal that will be achieved by executing this plan. */
	private Goal goal;
	
	/**
	 * Set the goal of this plan. If the provided argument is not a goal then 
	 * nothing happens. If the provided argument is a goal then it will be set 
	 * to being pursued, and no other plan scheme will be initiated for this goal 
	 * until it is not pursued anymore.  
	 * @param trigger
	 */
	public final void setPlanGoal(final Trigger trigger){
		if(trigger == null) throw new IllegalArgumentException("Trying to create a plan without a trigger.");
		if(trigger instanceof Goal){
			this.goal = (Goal)trigger;
			this.goal.setPursued(true);
		} else this.goal = null;
	}
	
	/**
	 * This check will ensure that if the plan was initiated for a specific goal, and
	 * that goal is no longer a goal, that then the plan will be abandoned. 
	 * 
	 * @param planInterface
	 * @return True iff there is no goal for this plan or if the agent still has the provided goal.
	 */
	public final boolean goalIsRelevant(final PlanToAgentInterface<T> planInterface){
		if(this.goal == null) return true;
		else {
			if(!planInterface.hasGoal(goal)){
				setFinished(true);
				return false;
			} else return true; 
		}
	}

	public final boolean isFinished(){ return this.finished; }
	
	/**
	 * Setting the argument to true will result in this plan being removed. 
	 * Setting the argument to false will ensure that the plan will be executed
	 * again during the next deliberation cycle. 
	 * @param finished
	 */
	protected final void setFinished(final boolean finished){ 
		this.finished = finished; 
		if(this.goal != null) // if the plan is finished then the goal is no longer pursued
			this.goal.setPursued(!finished); 
	}  
	
	/**
	 * Execute the business logic of the plan. Make sure that when you implement this
	 * method that the method will return. Otherwise it will hold up other agents that
	 * are executed in the same thread. Also, if the plan should only be executed once,
	 * then make sure that somewhere in the method it calls the setFinished(true) method.
	 * 
	 * @param planInterface
	 * @throws PlanExecutionError If you throw this error than it will be automatically adopted as an internal trigger.
	 */
	public abstract T execute(final PlanToAgentInterface<T> planInterface) throws PlanExecutionError;
	

	/** Token to indicate that an instantiate method for a plan was not fired by a trigger.*/
//	public static Plan<?> UNINSTANTIATED = new Plan<>(){ @Override
//		public final Object execute(final PlanToAgentInterface<Object> planInterface) throws PlanExecutionError {return null;}
//	};

	public static <T> Plan<T> UNINSTANTIATED() {
		return new Plan<T>() {
			@Override
			public T execute(PlanToAgentInterface<T> planInterface) throws PlanExecutionError {
				return null;
			}
		};
	}


}
