package nl.uu.cs.iss.ga.sim2apl.core.defaults.deliberationsteps;

import java.util.Iterator;
import java.util.List;

import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationStep;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Goal;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme;
import nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor;

/**
 * The default deliberation step adds to the deliberation interface a method to 
 * process a list of triggers given a list of plan schemes. It also stores an 
 * interface to the agent. 
 * 
 * @author Bas Testerink
 */
public abstract class DefaultDeliberationStep implements DeliberationStep {
	/** Interface to the agent. */
	protected final nl.uu.cs.iss.ga.sim2apl.core.agent.Agent agent;

	public  DefaultDeliberationStep(final Agent agent){
		this.agent = agent;
	}

	// Currently a goal differs from triggers in that a goal is permanent until its isAchieved(Context) method returns true.

	/** For each of the provided triggers and plan schemes, check whether the plan scheme is triggered by the trigger. If so, then the 
	 * plan scheme is applied. If the triggers are goals then they will  be skipped if they are 
	 * already pursued (i.e. a plan is already in existence for that goal). */
	protected final void applyPlanSchemes(final List<? extends nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger> triggers, final List<nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme> planSchemes){
		for(nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger trigger : triggers){
			// For goals check whether there is not already a plan instantiated for the goal. In this implementation each goal can have
			// at most one instantiated plan scheme that tries to achieve that goal.
			// TODO: this is different from 2APL, there it is checked FOR EACH rule whether that rule is already instantiated for
			// the goal. Hence multiple plan schemes could be instantiated for the same goal. However, this is very rarely used
			// and highly inefficient.  
			if(!(trigger instanceof nl.uu.cs.iss.ga.sim2apl.core.agent.Goal && ((nl.uu.cs.iss.ga.sim2apl.core.agent.Goal)trigger).isPursued())){
				for(PlanScheme planScheme : planSchemes){
					if(this.agent.tryApplication(trigger, planScheme)){
						break;
					}
				}
			}
		}
	}
	/**
	 * For each of the provided triggers and trigger interceptors, check whether the interceptor is triggered by the trigger. If so, 
	 * then the interceptor is removed. If the interceptor consumes the trigger, then the trigger is also removed. An exception is with
	 * goals. Goals can only be removed if they are achieved, hence a goal is not removed, even if it triggers a consuming interceptor.
	 * Note that the list of triggers is possibly changed by this call. It is intended that interceptors are applied before plan schemes. 
	 * @param triggers
	 * @param interceptors
	 */
	protected final void applyTriggerInterceptors(final List<? extends nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger> triggers, final Iterator<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor> interceptors){
		while(interceptors.hasNext()){
			TriggerInterceptor interceptor = interceptors.next();
			Iterator<? extends nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger> triggerIterator = triggers.iterator();
			while(triggerIterator.hasNext()){
				Trigger trigger = triggerIterator.next();
				if(this.agent.tryApplication(trigger, interceptor)){ 
					interceptors.remove();
					if(interceptor.isTriggerConsuming() && !(trigger instanceof Goal)){
						triggerIterator.remove();
					} 
					break;
				}
			} 
		}
	}
}