package nl.uu.cs.iss.ga.sim2apl.core.plan;


/**
 * The following description holds for the 2APL default deliberation cycle. If you change the deliberation 
 * steps then this does not necessarily hold anymore.
 * 
 * A trigger interceptor can be interpreted as a temporary plan scheme. These interceptors are 
 * tried before the plan schemes are tried. If an interceptor is fired then it may or may not 
 * consume the trigger. Interceptors can be added to an agent from the planToAgentInterface, 
 * i.e. this is something that is part of plan. When an interceptor is fired, then it will be 
 * removed.
 * 
 * An example usage of an intercepter is to create a wait-for-message construct. For instance, 
 * if in a plan there is a moment where the plan has to wait for a message, then the plan may 
 * create a trigger interceptor with itself as the plan to be executed when the correct trigger 
 * is detected. 
 * 
 * Just like plan schemes, interceptors are categorized for goals, messages, external events and 
 * internal events. 
 * 
 * The PlanToAgentInterface class contains several methods that under the hood make use of interceptors. 
 * These methods include while loops that span multiple deliberation cycles, wait-for-trigger constructs, 
 * and wait-on-process constructs. 
 * Check the available methods of that class before making your own interceptors. 
 * 
 * @author Bas Testerink
 */
public abstract class TriggerInterceptor<T> implements PlanScheme<T> {
	/** If set to true, then the trigger that triggers this interceptor should be consumed during the deliberation cycle. */
	private final boolean consumesTrigger;
	
	/**
	 * Constructor.
	 * @param consumesTrigger If set to true, then the trigger that triggers this interceptor should be consumed during the deliberation cycle.
	 */
	public TriggerInterceptor(final boolean consumesTrigger){
		this.consumesTrigger = consumesTrigger;
	}
	
	/** 
	 * @return True iff  triggers that fire this interceptor should be consumed. 
	 */
	public final boolean isTriggerConsuming(){
		return this.consumesTrigger;
	}
}
