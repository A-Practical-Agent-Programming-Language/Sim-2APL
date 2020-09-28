package nl.uu.cs.iss.ga.sim2apl.core.plan.builtin;

import java.util.ArrayList; 
import java.util.List;
import java.util.function.Predicate;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentContextInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;
import nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor;

/**
 * An enhanced trigger interceptor is an extension of a trigger interceptor. The extension consists of the 
 * capability to add other interceptors which ought to be removed when this interceptor fires. The instantiate 
 * method of an enhanced trigger interceptor does not make use of the context of the agent, instead, the programmer 
 * has to provide a predicate over Triggers which will be used to check whether the interceptor fires.
 * 
 * The plan of the interceptor is also given by the programmer, though it is wrapped in another plan that causes the 
 * deletion of other interceptors, if applicable. 
 * 
 * 
 * This class is used by the PlanToAgentInterface in order to implement for instance waitForX-functionalities.
 * @author Bas Testerink
 *
 */
public final class EnhancedTriggerInterceptor<T> extends nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> {
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> goalInterceptorsToRemove,
	  									  externalTriggerInterceptorsToRemove,
	  									  internalTriggerInterceptorsToRemove,
	  									  messageInterceptorsToRemove;
	private final Predicate<Trigger> selector;
	private final DecoupledPlan<T> plan;
	
	public EnhancedTriggerInterceptor(final boolean consumesTrigger, final Predicate<Trigger> selector, DecoupledPlan<T> plan){
		super(consumesTrigger);
		this.selector = selector; 
		this.plan = plan;
		this.goalInterceptorsToRemove = new ArrayList<>();
		this.externalTriggerInterceptorsToRemove = new ArrayList<>();
		this.internalTriggerInterceptorsToRemove = new ArrayList<>();
		this.messageInterceptorsToRemove = new ArrayList<>();
	}
	
	@Override
	public final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T> instantiate(final Trigger trigger, final AgentContextInterface<T> contextInterface){
		if(this.selector.test(trigger)){
			return new ExtendedInterceptorPlan<T>(this.plan, trigger,
					this.goalInterceptorsToRemove,
					this.externalTriggerInterceptorsToRemove,
					this.internalTriggerInterceptorsToRemove,
					this.messageInterceptorsToRemove);
		}
		return Plan.UNINSTANTIATED();
	}

	/** If this interceptor is fired, then the provided interceptor is removed from the list of goal interceptors when this interceptor's plan is executed. */
	public final void addGoalInterceptorToRemove(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.goalInterceptorsToRemove.add(interceptor);
	}

	/** If this interceptor is fired, then the provided interceptor is removed from the list of external trigger interceptors when this interceptor's plan is executed. */
	public final void addExternalTriggerInterceptorToRemove(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.externalTriggerInterceptorsToRemove.add(interceptor);
	}

	/** If this interceptor is fired, then the provided interceptor is removed from the list of message interceptors when this interceptor's plan is executed. */
	public final void addInternalTriggerInterceptorToRemove(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.internalTriggerInterceptorsToRemove.add(interceptor);
	}

	/** If this interceptor is fired, then the provided interceptor is removed from the list of internal trigger interceptors when this interceptor's plan is executed. */
	public final void addMessageInterceptorToRemove(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.messageInterceptorsToRemove.add(interceptor);
	}
	
	private final class ExtendedInterceptorPlan<T> extends Plan<T> {
		public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> goalInterceptorsToRemove,
											  externalTriggerInterceptorsToRemove,
											  internalTriggerInterceptorsToRemove,
											  messageInterceptorsToRemove;
		
		private final DecoupledPlan<T> plan;
		private final Trigger trigger;
		private boolean firstExecute;
		
		public ExtendedInterceptorPlan(final DecoupledPlan<T> plan, final Trigger trigger,
				final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> goalInterceptorsToRemove,
				final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> externalTriggerInterceptorsToRemove,
				final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> internalTriggerInterceptorsToRemove,
				final List<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> messageInterceptorsToRemove){
			this.plan = plan;
			this.trigger = trigger; 
			this.goalInterceptorsToRemove = goalInterceptorsToRemove;
			this.externalTriggerInterceptorsToRemove = externalTriggerInterceptorsToRemove;
			this.internalTriggerInterceptorsToRemove = internalTriggerInterceptorsToRemove;
			this.messageInterceptorsToRemove = messageInterceptorsToRemove;
			this.firstExecute = false;
		}
		
		@Override
		public final T execute(final PlanToAgentInterface<T> planInterface) throws PlanExecutionError {
			// Only execute the removal of other interceptors the first time that this plan is executed
			if(this.firstExecute){
				for(nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor : this.goalInterceptorsToRemove)
					planInterface.removeGoalInterceptor(interceptor);
				for(nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor : this.externalTriggerInterceptorsToRemove)
					planInterface.removeExternalTriggerInterceptor(interceptor);
				for(nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor : this.internalTriggerInterceptorsToRemove)
					planInterface.removeInternalTriggerInterceptor(interceptor);
				for(TriggerInterceptor<T> interceptor : this.messageInterceptorsToRemove)
					planInterface.removeMessageInterceptor(interceptor);
				this.firstExecute = false;
			}
			// Then proceed as if this plan is the provided plan when this interceptor was created
			T planAction = this.plan.execute(this.trigger, planInterface);
			setFinished(this.plan.isFinished());
			return planAction;
		}
	}
}