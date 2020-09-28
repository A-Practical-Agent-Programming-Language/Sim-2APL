package nl.uu.cs.iss.ga.sim2apl.core.agent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import nl.uu.cs.iss.ga.sim2apl.core.defaults.messenger.MessageReceiverNotFoundException;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageInterface;
import nl.uu.cs.iss.ga.sim2apl.core.platform.PlatformNotFoundException;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;
import nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor;
import nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.*;

/**
 * Exposes all functionalities to an agent that the execution of a plan might need/is allowed
 * to call upon according to 2APL standards. 
 * 
 * @author Bas Testerink 
 */
public final class PlanToAgentInterface<T> {
	/** The agent that is exposed by this interface. */
	private final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent<T> agent;
	
	public PlanToAgentInterface(final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent<T> agent){
		this.agent = agent;
	}

	// TODO: This is a hack, resolve this (probably by deleting or reworking the whole planinterface concept).
	public final Agent<T> getAgent() {
		return agent;
	}
	
	/** Obtain the id of the agent that is exposed through this interface. */
	public final nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID getAgentID(){ return this.agent.getAID(); }
	
	/** Obtain the context that belongs to a certain class. */
	public final <C extends Context> C getContext(final Class<C> klass){ return this.agent.getContext(klass); }
	
	/** Check whether the list of current goals contains the provided argument goal. */
	public final boolean hasGoal(final nl.uu.cs.iss.ga.sim2apl.core.agent.Goal goal){ return this.agent.hasGoal(goal); }
	
	/** Remove the provided goal from the list of current goals. */
	public final void dropGoal(final nl.uu.cs.iss.ga.sim2apl.core.agent.Goal goal){ this.agent.dropGoal(goal); }
	
	/** Add a goal to the list of current goals. Will check whether the list of 
	 * current goals already contains the provided goal. */
	public final void adoptGoal(final nl.uu.cs.iss.ga.sim2apl.core.agent.Goal goal){ this.agent.adoptGoal(goal); }
	
	/** Add a plan to the list of current plans. This plan will be executed during
	 * the next "execute plans" deliberation step. */
	public final void adoptPlan(final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T> plan){ this.agent.adoptPlan(plan); }
	
	/** Add an internal trigger to the list of current internal triggers. This trigger 
	 * will be processed during the next deliberation cycle.*/
	public final void addInternalTrigger(final Trigger trigger){ this.agent.addInternalTrigger(trigger); }
	
	/**
	 * By default an agent is never finished, unless this method is called explicitly
	 * from within a plan. If this method is called then the agent will be killed and 
	 * removed from the platform before it can start a new deliberation cycle.
	 */
	public final void finished(){ this.agent.finished(); } // The agent is finished with its execution
	
	/** Add an interceptor for goals. */
	public final void adoptGoalInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.adoptGoalInterceptor(interceptor);
	}
	
	/** Add an interceptor for external triggers. */
	public final void adoptExternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.adoptExternalTriggerInterceptor(interceptor);
	}

	/** Add an interceptor for internal triggers. */
	public final void adoptInternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.adoptInternalTriggerInterceptor(interceptor);
	}
	
	/** Add an interceptor for messages. */
	public final void adoptMessageInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.adoptMessageInterceptor(interceptor);
	}

	public final void removeGoalInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.removeGoalInterceptor(interceptor);
	}
	public final void removeExternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.removeExternalTriggerInterceptor(interceptor);
	}
	public final void removeInternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.removeInternalTriggerInterceptor(interceptor);
	}
	public final void removeMessageInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.agent.removeMessageInterceptor(interceptor);
	}
	
	/** Send a message through the agent's messenger client. */
	public final void sendMessage(final AgentID receiver, final MessageInterface message) {
		try {
			this.agent.sendMessage(receiver, message);
		} catch (MessageReceiverNotFoundException | PlatformNotFoundException e) {
			e.printStackTrace();
		}
	} 
	
	/** 
	 * Upon calling this method an interceptor is created such that it fires if the predicate holds for a given trigger and its plan contains the 
	 * given decoupled plan. The plan is a run-once plan which is set to finished after a single execution. The trigger that fires the interceptor is consumed (i.e. removed). 
	 * This method adds the interceptor to the agent, there is no need to call upon the planInterface the adopt TriggerInterceptor method after 
	 * this method is called. One can use the returned interceptor to for instance make it mutually exclusive with other interceptors.
	 */
	public final <T extends Trigger> nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor waitForMessage(final Predicate<Trigger> selector, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.DecoupledPlanBodyInterface<T> plan){
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptor =  (new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptorBuilder()).setSelector(selector).setConsuming(true).setForceRunOnce(true).setPlan(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.InstantiableRunOnceDecoupledPlan<T>(plan)).build();
		adoptMessageInterceptor(interceptor);
		return interceptor; 
	}
	/** 
	 * Upon calling this method an interceptor is created such that it fires if the predicate holds for a given trigger and its plan contains the 
	 * given decoupled plan. The plan is a run-once plan which is set to finished after a single execution. The trigger that fires the interceptor is consumed (i.e. removed). 
	 * This method adds the interceptor to the agent, there is no need to call upon the planInterface the adopt TriggerInterceptor method after 
	 * this method is called. One can use the returned interceptor to for instance make it mutually exclusive with other interceptors.
	 */
	public final <T extends Trigger> nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor waitForExternalTrigger(final Predicate<Trigger> selector, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.DecoupledPlanBodyInterface<T> plan){
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptor =
				(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptorBuilder())
				.setSelector(selector)
				.setConsuming(true).setForceRunOnce(true).setPlan(
				new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.InstantiableRunOnceDecoupledPlan<>(plan)).build();
		adoptExternalTriggerInterceptor(interceptor);
		return interceptor; 
	}
	/** 
	 * Upon calling this method an interceptor is created such that it fires if the predicate holds for a given trigger and its plan contains the 
	 * given decoupled plan. The plan is a run-once plan which is set to finished after a single execution. The trigger that fires the interceptor is consumed (i.e. removed). 
	 * This method adds the interceptor to the agent, there is no need to call upon the planInterface the adopt TriggerInterceptor method after 
	 * this method is called. One can use the returned interceptor to for instance make it mutually exclusive with other interceptors.
	 */
	public final <T extends Trigger> nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor waitForInternalTrigger(final Predicate<Trigger> selector, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.DecoupledPlanBodyInterface<T> plan){
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptor =  (new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptorBuilder()).setSelector(selector).setConsuming(true).setForceRunOnce(true).setPlan(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.InstantiableRunOnceDecoupledPlan<T>(plan)).build();
		adoptInternalTriggerInterceptor(interceptor);
		return interceptor; 
	}
	/** 
	 * Upon calling this method an interceptor is created such that it fires if the predicate holds for a given trigger and its plan contains the 
	 * given decoupled plan. The plan is a run-once plan which is set to finished after a single execution. The trigger that fires the interceptor is consumed (i.e. removed). 
	 * This method adds the interceptor to the agent, there is no need to call upon the planInterface the adopt TriggerInterceptor method after 
	 * this method is called. One can use the returned interceptor to for instance make it mutually exclusive with other interceptors.
	 */
	public final <T extends Trigger> nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor waitForGoal(final Predicate<Trigger> selector, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.DecoupledPlanBodyInterface<T> plan){
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptor =  (new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptorBuilder()).setSelector(selector).setConsuming(true).setForceRunOnce(true).setPlan(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.InstantiableRunOnceDecoupledPlan<T>(plan)).build();
		adoptGoalInterceptor(interceptor);
		return interceptor; 
	}
	
	/** If one of the given TriggerInterceptors fires, then the other is removed. For each interceptor its instances are removed from the goal, external trigger, 
	 * internal trigger and message interceptors. Note that the interceptors are removed when the plan of an interceptor is executed. Hence, if mutually exclusive 
	 * interceptors fire in the same deliberation cycle, then all their plans are still executed. The intended use of this method is that the provided interceptors 
	 * are triggered by triggers which are mutually exclusive in terms of whether they can be received in the same deliberation cycle. E.g. upon sending an offer, 
	 * one can make the interceptors that handle a reject or accept notification mutually exclusive, as the expected trigger is either a reject or (exclusively) an 
	 * accept.*/
	public final static void makeMutuallyExclusive(final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorA, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorB){
		interceptorARemovesB(interceptorA, interceptorB);
		interceptorARemovesB(interceptorB, interceptorA);
	}
	/** If one of the given TriggerInterceptors fails, then the others are removed. For each interceptor it is removed whether it is part of the goal, external trigger, 
	 * internal trigger or message interceptors. Note that the interceptors are removed when the plan of an interceptor is executed. Hence, if mutually exclusive 
	 * interceptors fire in the same deliberation cycle, then all their plans are still executed. The intended use of this method is that the provided interceptors 
	 * are triggered by triggers which are mutually exclusive in terms of whether they can be received in the same deliberation cycle. E.g. upon sending an offer, 
	 * one can make the interceptors that handle a reject or accept notification mutually exclusive, as the expected trigger is either a reject or (exclusively) an 
	 * accept. */
	public final static void makeMutuallyExclusive(final Collection<nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor> interceptors){
		for(nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorA : interceptors)
			for(nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorB : interceptors)
				if(interceptorA != interceptorB)
					interceptorARemovesB(interceptorA, interceptorB);
	}
	/** Auxiliary method to add one interceptor to the to-remove lists of the other.  */
	private final static void interceptorARemovesB(final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorA, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.EnhancedTriggerInterceptor interceptorB){
		interceptorA.addExternalTriggerInterceptorToRemove(interceptorB);
		interceptorA.addInternalTriggerInterceptorToRemove(interceptorB);
		interceptorA.addGoalInterceptorToRemove(interceptorB);
		interceptorA.addMessageInterceptorToRemove(interceptorB);
	}

	/** Remove the given goal until a trigger of the given class is received or adopted as goal, at which point the goal is adopted again. This does not
	 * consume the trigger. */
	public final <T extends Trigger> void suspendGoalUntil(final nl.uu.cs.iss.ga.sim2apl.core.agent.Goal goal, final Class<T> triggerClass){
		suspendGoalUntil(goal, (Trigger t) -> {return triggerClass.isInstance(t);});
	} 
	
	/** Remove the given goal until a trigger is received (or adopted as goal) that fulfills the condition, at which point the goal is adopted again. This does not 
	 * consume the trigger. The predicate is applied upon any incoming/received/adopted message, internal trigger, external trigger and goal. */
	public final void suspendGoalUntil(final Goal goal, final Predicate<Trigger> condition){
		// Remove the goal for now
		dropGoal(goal); 
		
		// Make the interceptor that adopts the goal again
		EnhancedTriggerInterceptor interceptor =  (new EnhancedTriggerInterceptorBuilder())
				.setConsuming(false)
				.setForceRunOnce(true)
				.setSelector(condition)
				// The plan is to simply adopt the goal again
				.setPlan(new InstantiableRunOnceDecoupledPlan<Trigger>(
						(Trigger trigger, PlanToAgentInterface planInt) -> {planInt.adoptGoal(goal); return null;}))
				.build();
		
		// Add the interceptor to the agent
		adoptGoalInterceptor(interceptor);
		adoptExternalTriggerInterceptor(interceptor);
		adoptInternalTriggerInterceptor(interceptor);
		adoptMessageInterceptor(interceptor);

		// Make sure that if the interceptor fires, then it is removed from all the interceptor lists
		interceptorARemovesB(interceptor, interceptor);
	}

	/** Implements a while loop that possibly lasts over several deliberation cycles. The provide plan will be rescheduled for the next cycle as long as the 
	 * condition holds. The condition is immediately checked and the plan possibly executed upon the call of this method. Note that code which comes after this 
	 * method call will be executed immediately, regardless of whether the condition holds or not.	 */
	public final Object repeatWhile(final Predicate<PlanToAgentInterface> condition, final SubPlanInterface plan) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {
		Object lastIntendedAction = null;
		if(condition.test(this)){ // If the condition holds
			lastIntendedAction = plan.execute(this);   // Then execute the plan
			// Reschedule another repeat-while call for the next cycle: 
			suspendToNextDeliberationCycle(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.RunOncePlan(){
				@Override
				public Object executeOnce(final PlanToAgentInterface planInt) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {
					return planInt.repeatWhile(condition, plan);
				}
			});
		}
		return lastIntendedAction;
	}
	
	/** Suspend a plan to the next deliberation cycle. This is ideal if for instance other current plans should be executed first.  */
	public final void suspendToNextDeliberationCycle(final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan plan){
		// Make the interceptor that will execute the plan
		nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor interceptor = new TriggerInterceptor(false) {
			@Override
			public nl.uu.cs.iss.ga.sim2apl.core.plan.Plan instantiate(Trigger trigger, AgentContextInterface contextInterface) {
				if(trigger == nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.SuspensionTrigger.getInstance()){
					return plan;
				}
				return null;
			}
		};
		// Adopt the interceptor
		adoptInternalTriggerInterceptor(interceptor);
		
		// Ensure that it will be triggered in the next deliberation cycle
		addInternalTrigger(SuspensionTrigger.getInstance());
	}
	
	/** Upon this method call the task will be scheduled in the agent's concurrency context (if any exists, if not, then an exception is thrown. To add a 
	 * concurrency context, you have to do this in the agent component factory). If the task is completed, then the given internal trigger will be added to the 
	 * agent's internal trigger list. */
	public final void getNotifiedWhenFinished(final Runnable task, Trigger internalTrigger) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			task.run(); 
			addInternalTrigger(internalTrigger);
		}); 
	}
	
	/** Upon this method call the task will be scheduled in the agent's concurrency context (if any exists, if not, then an exception is thrown. To add a 
	 * concurrency context, you have to do this in the agent component factory). If the task is completed, then the given internal trigger will be added to the 
	 * agent's internal trigger list. The provide internal trigger will be set with the value that the task returns.  
	 * @param <V>*/
	public final <V> void getNotifiedWhenFinished(final Callable<V> task, nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.InternalTriggerReturnValue<V> internalTrigger) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			try {
				internalTrigger.setValue(task.call());
				addInternalTrigger(internalTrigger);
			} catch (Exception e) { 
				// TODO: adopt internal error with the exception
				e.printStackTrace();
			} 
			addInternalTrigger(internalTrigger);
		});
	}
	
	/** Submit in the concurrency context a runnable that waits until the given task is finished. The return value of the task is
	 *  loaded in the given internal trigger and adopted as an internal trigger afterwards.  */
	public final <V> void getNotifiedWhenFinished(final Future<V> task, InternalTriggerReturnValue<V> internalTrigger) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			while(!task.isDone());									// Wait until the task is done
			try {
				internalTrigger.setValue(task.get());				// Then set the trigger's value
				addInternalTrigger(internalTrigger);				// And add it to the agent for processing
			} catch (Exception e) { 
				// TODO: adopt internal error with the exception
				e.printStackTrace();
			} 									
		});	
	}
	
	/** Will attempt to execute the task in the concurrency context (if it fails it throws an error). If the task is completed, then the given plan is 
	 * adopted. 
	 * @throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException */
	public final void adoptPlanWhenFinished(final Runnable task, final Plan plan) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			task.run();
			this.agent.asynchronousAdoptPlan(plan);
		}); 
	}
	
	/** Will attempt to execute the task in the concurrency context (if it fails it throws an error). If the task is completed, then the given plan body is 
	 * put inside a plan that execute once (i.e. is automatically set to being finished after a single execution) with the result of the task.
	 * @throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException */
	public final <V> void adoptPlanWhenFinished(final Callable<V> task, final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.DecoupledPlanBodyInterface<V> plan) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			try {
				V value = task.call();									// Wait until the task is done
				this.agent.asynchronousAdoptPlan(new nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.RunOncePlan(){
					@Override
					public Object executeOnce(PlanToAgentInterface planInt) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {
						return plan.execute(value, planInt);
					} 
				});
			} catch (Exception e) { 
				// TODO: adopt internal error with the exception
				e.printStackTrace();
			} 														 
		}); 
	}
	
	/** Will attempt to wait on the future by using the concurrency context (if it fails it throws an error). If the task is completed, then the given plan body is 
	 * put inside a plan that execute once (i.e. is automatically set to being finished after a single execution) with the result of the future.
	 * @throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException */
	public final <V> void adoptPlanWhenFinished(final Future<V> task, final DecoupledPlanBodyInterface<V> plan) throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getConcurrencyContext();
		context.getExecutor().submit(()-> {
			while(!task.isDone());									// Wait until the task is done
			try {
				V value = task.get();
				this.agent.asynchronousAdoptPlan(new RunOncePlan(){
					@Override
					public Object executeOnce(PlanToAgentInterface planInt) throws PlanExecutionError {
						return plan.execute(value, planInt);
					} 
				});
			} catch (Exception e) { 
				// TODO: adopt internal error with the exception
				e.printStackTrace();
			} 														 
		}); 
	}
	
	/** Get the concurrency context from the agent or throw an exception if none exists. */
	private final nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext getConcurrencyContext() throws nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.NoConcurrencyContextException {
		nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.ConcurrencyContext context = getContext(ConcurrencyContext.class);
		if(context == null) throw new NoConcurrencyContextException();
		else return context;
	}
} 
