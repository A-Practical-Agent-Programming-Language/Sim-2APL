package nl.uu.cs.iss.ga.sim2apl.core.agent;

import nl.uu.cs.iss.ga.sim2apl.core.defaults.deliberationsteps.*;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationActionStep;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationStep;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanSchemeBase;
import nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.FunctionalPlanScheme;
import nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.FunctionalPlanSchemeInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AgentArguments<T> {

	private final List<PlanScheme<T>> goalPlanSchemes, internalTriggerPlanSchemes, externalTriggerPlanSchemes, messagePlanSchemes;
	private final List<Context> contexts;
	private final HashMap<Context, Class<? extends Context>[]> explicitKeyContexts;
	private final List<Plan<T>> initialPlans;
	private final List<Plan<T>> downPlans;
		
	public AgentArguments(){
		this.goalPlanSchemes = new ArrayList<>();
		this.internalTriggerPlanSchemes = new ArrayList<>();
		this.externalTriggerPlanSchemes = new ArrayList<>();
		this.messagePlanSchemes = new ArrayList<>();
		this.contexts = new ArrayList<>();
		this.explicitKeyContexts = new HashMap<>();
		this.initialPlans = new ArrayList<>();
		this.downPlans = new ArrayList<>();
	}
	
	/** Builds the plan scheme base. This is intentionally package-only so that a programmer cannot accidentally mess with the plan scheme base. */
	final PlanSchemeBase<T> createPlanSchemeBase(){
		return new PlanSchemeBase<T>(this.goalPlanSchemes, this.internalTriggerPlanSchemes, this.externalTriggerPlanSchemes, this.messagePlanSchemes);
	}

	/** Builds the context container. This is intentionally package-only so that a programmer cannot accidentally mess with the container. */
	final ContextContainer createContextContainer(){
		ContextContainer container = new ContextContainer();
		for(Context context : this.contexts)
			container.addContext(context);
		for(Context context : this.explicitKeyContexts.keySet()) {
			container.addImplementedContext(context, this.explicitKeyContexts.get(context));
		}
		return container;
	}
	
	/** Produce the sense and reason parts of the deliberation cycle of the agent.
	 * The provided interface can be used by deliberation steps to perform their functionalities on the agent.
	 * The default implementation is that the 2APL deliberation cycle is used:
	 * ApplyGoalPlanSchemes -> ApplyExternalTriggerPlanSchemes ->
	 *  ApplyInternalTriggerPlanSchemes -> ApplyMessagePlanSchemes -> ExecutePlans.
	 *  For Sim2APL, the ExecutePlans step is moved to the act Cycle*/
	final List<DeliberationStep> createSenseReasonCycle(final Agent<T> agent){
		// Produces the default 2APL deliberation cycle.
		List<DeliberationStep> senseReasonCycle = new ArrayList<>();
		senseReasonCycle.add(new ApplyGoalPlanSchemes(agent));
		senseReasonCycle.add(new ApplyExternalTriggerPlanSchemes(agent));
		senseReasonCycle.add(new ApplyInternalTriggerPlanSchemes(agent));
		senseReasonCycle.add(new ApplyMessagePlanSchemes(agent));
		return senseReasonCycle;
	}

	/**
	 * Produce the act parts of the deliberation cycle of the agent. This cycle produces actions, which is why
	 * it is decoupled from the rest of the deliberation cycle
	 * @param agent
	 * @return
	 */
	final List<DeliberationActionStep<T>> createActCycle(final Agent<T> agent) {
		List<DeliberationActionStep<T>> actCycle = new ArrayList<>();
		actCycle.add(new ExecutePlans<>(agent));
		return actCycle;
	}
	
	/** Returns a list of plans that will be executed upon the agent's first deliberation cycle. */
	final List<Plan<T>> getInitialPlans(){
		return new ArrayList<>(this.initialPlans); // Ensure that no further additions will affect the the agent after creation
	}
	
	/** Returns a list of plans that will be executed after the agent's last deliberation cycle. */
	final List<Plan<T>> getShutdownPlans(){
		return new ArrayList<>(this.downPlans); // Ensure that no further additions will affect the the agent after creation
	} 
	
	// Filling the builder
	/** Add a plan scheme that processes external triggers. */
	public final AgentArguments<T> addExternalTriggerPlanScheme(final PlanScheme<T> planScheme){ this.externalTriggerPlanSchemes.add(planScheme); return this; }
	/** Add a plan scheme that processes internal triggers. */
	public final AgentArguments<T> addInternalTriggerPlanScheme(final PlanScheme<T> planScheme){ this.internalTriggerPlanSchemes.add(planScheme); return this; }
	/** Add a plan scheme that processes messages. */
	public final AgentArguments<T> addMessagePlanScheme(final PlanScheme<T> planScheme){ this.messagePlanSchemes.add(planScheme); return this; }
	/** Add a plan scheme that try to achieve goals. */
	public final AgentArguments<T> addGoalPlanScheme(final PlanScheme<T> planScheme){ this.goalPlanSchemes.add(planScheme); return this; }
	/** Add a plan scheme that processes external triggers. */
	public final AgentArguments<T> addExternalTriggerPlanScheme(final FunctionalPlanSchemeInterface<T> planScheme){ this.externalTriggerPlanSchemes.add(new FunctionalPlanScheme<T>(planScheme)); return this; }
	/** Add a plan scheme that processes internal triggers. */
	public final AgentArguments<T> addInternalTriggerPlanScheme(final FunctionalPlanSchemeInterface<T> planScheme){ this.internalTriggerPlanSchemes.add(new FunctionalPlanScheme<T>(planScheme)); return this; }
	/** Add a plan scheme that processes messages. */
	public final AgentArguments<T> addMessagePlanScheme(final FunctionalPlanSchemeInterface<T> planScheme){ this.messagePlanSchemes.add(new FunctionalPlanScheme<T>(planScheme)); return this; }
	/** Add a plan scheme that try to achieve goals. */
	public final AgentArguments<T> addGoalPlanScheme(final FunctionalPlanSchemeInterface<T> planScheme){ this.goalPlanSchemes.add(new FunctionalPlanScheme<T>(planScheme)); return this; }
	/** Add a context that is used for decision making and plan execution. */
	public final AgentArguments<T> addContext(final Context context){ this.contexts.add(context); return this; }
	/** Add a context that is used for decision making and plan execution with one or more explicit lookup keys. */
	public final AgentArguments<T> addContext(final Context context, Class<? extends Context> ... keys){ this.explicitKeyContexts.put(context, keys); return this; }
	/** Add a plan that will be executed in the first deliberation cycle. */
	public final AgentArguments<T> addInitialPlan(final Plan<T> plan){ this.initialPlans.add(plan); return this; }
	/** Add a plan that will be executed after the last deliberation cycle this agent will participate in. */
	public final AgentArguments<T> addShutdownPlan(final Plan<T> plan){ this.downPlans.add(plan); return this; }
	 
	/** Copies the planschemes, contexts and initial plan of another 
	 * builder into this builder. This can be used to for instance include a 
	 * builder that represents a premade set of plan schemes, etc, that forms a 
	 * coherent capability. */
	public final AgentArguments<T> include(final AgentArguments<T> builder){
		this.externalTriggerPlanSchemes.addAll(builder.externalTriggerPlanSchemes);
		this.internalTriggerPlanSchemes.addAll(builder.internalTriggerPlanSchemes);
		this.messagePlanSchemes.addAll(builder.messagePlanSchemes);
		this.goalPlanSchemes.addAll(builder.goalPlanSchemes);
		this.initialPlans.addAll(builder.initialPlans);
		this.downPlans.addAll(builder.downPlans);
		this.contexts.addAll(builder.contexts);
		this.explicitKeyContexts.putAll(builder.explicitKeyContexts);
		return this;
	}
}
