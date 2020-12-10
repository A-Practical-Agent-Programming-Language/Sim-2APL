package nl.uu.cs.iss.ga.sim2apl.core.agent;

import nl.uu.cs.iss.ga.sim2apl.core.defaults.messenger.MessageReceiverNotFoundException;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationActionStep;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationStep;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.SelfRescheduler;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.FIPAAgentState;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageInterface;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageLog;
import nl.uu.cs.iss.ga.sim2apl.core.logging.MessageLogContext;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanSchemeBase;
import nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor;
import nl.uu.cs.iss.ga.sim2apl.core.platform.Platform;
import nl.uu.cs.iss.ga.sim2apl.core.platform.PlatformNotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;


/**
 * This class represents a NET2APL Agent.
 * This class complies with FIPA (Agent)
 * @author Mohammad Shafahi
 */
public class Agent<T> implements AgentInterface{
	
	private Platform platform =null;
	
	/** Interface that exposes the relevant parts of the agent run time data for plans. */
	private final PlanToAgentInterface<T> planInterface;
	
	private AgentID AID;

	/** Listeners that are notified when this agent dies. */
	private final List<AgentDeathListener> deathListeners;
	
	private Queue<MessageInterface> messageQueue;
	
	/** The messageHistory contains the history of messages send and received by the Agent
	 */
	private final MessageLogContext messageContext;
	 
	// This indicates the state of the agent (The naming is FIPA complient).
	private FIPAAgentState State = FIPAAgentState.INITIATED;

	/** The context container which contains contexts for decision making and actuation. */
	private final ContextContainer contextContainer;
	
	/** The current goals. */
	private final List<Goal> goals;
	
	/** The current internal and external  triggers. */
	private final List<Trigger> internalTriggers, externalTriggers;
	
	/** The current trigger interceptors. */
	private List<TriggerInterceptor<T>> internalTriggerInterceptors, externalTriggerInterceptors, messageInterceptors, goalInterceptors;
	
	/** The agent's plan scheme base that defines its decision making. */
	private final PlanSchemeBase<T> planSchemeBase;
	
	/** The current plans of the agent. */
	private final List<nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T>> plans;
	
	/** The agent will try to execute these on shutdown/kill. */
	private final List<nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T>> downPlans;
	
	/** The sense-reason part of the deliberation cycle of the agent. */
	private final List<DeliberationStep> senseReasonCycle;

	/** The act part of the deliberation cycle of the agent. */
	private final List<DeliberationActionStep<T>> actCycle;
	
	/** Whether the agent is forced to stop, is finished, or is sleeping. */
	private boolean forciblyStop, finished;
	
	/** Interface that exposes the context container of this agent. Is given to goals for checking whether they are achieved. */
	private final AgentContextInterface<T> contextInterface;
	
	/** Interface to the platform that allows the agent to reschedule its own deliberation runnable. */
	private SelfRescheduler rescheduler = null;

	public Agent(Platform p, nl.uu.cs.iss.ga.sim2apl.core.agent.AgentArguments<T> args, AgentID agentID) {
		
		this.AID = agentID;
		this.contextContainer = args.createContextContainer();
		this.goals = new ArrayList<>();
		this.internalTriggers = new ArrayList<>();
		this.externalTriggers = new ArrayList<>();
		this.internalTriggerInterceptors = new ArrayList<>();
		this.externalTriggerInterceptors = new ArrayList<>();
		this.messageInterceptors = new ArrayList<>();
		this.goalInterceptors = new ArrayList<>();
		this.planSchemeBase = args.createPlanSchemeBase();
		this.plans = new ArrayList<>();
		this.downPlans = new ArrayList<>();
		this.senseReasonCycle = Collections.unmodifiableList(args.createSenseReasonCycle(this));
		this.actCycle = Collections.unmodifiableList(args.createActCycle(this));
		this.contextInterface = new AgentContextInterface<T>(this);

		this.messageQueue = new ConcurrentLinkedQueue<>();
		this.deathListeners = new ArrayList<>();
		this.planInterface = new PlanToAgentInterface<T>(this);
		
		this.plans.addAll(args.getInitialPlans());
		this.downPlans.addAll(args.getShutdownPlans());
		
		this.messageContext = new MessageLogContext();
		this.contextContainer.addContext(messageContext);
		
		p.register(this);
	}

	public Agent(Platform p, AgentArguments<T> args) throws URISyntaxException {
		this(p, args, new AgentID(UUID.randomUUID(), p.getHost(), p.getPort()));
	}
		
	public AgentID getAID() {
		return AID;
	}

	public void setAID(AgentID aID) {
		AID = aID;
	}

	public URI getName() {
		return this.AID.getName();
	}

	/**
	 * Invokes the agent, meaning the agent is lifted from its INITIATED state to the ACTIVE state, according to
	 * FIPA standards: http://www.fipa.org/specs/fipa00023/SC00023J.html#_Ref449500188
	 */
	public void invoke() {
		this.State = FIPAAgentState.ACTIVE;
	}

	//An agent receives a message using this function. 
	//The assumption here is that if the agent is in waiting or suspended it will change states to active to receive the message
	@Override
	public synchronized void receiveMessage(MessageInterface message) {
		this.messageQueue.add(message);
		this.messageContext.addReceivedMessage(message);
		this.checkWhetherToReschedule();
    }
	
	@SuppressWarnings("unchecked")
	public <T extends MessageInterface> MessageLog sendMessage(T message)
			throws MessageReceiverNotFoundException, PlatformNotFoundException {
		message.addUserDefinedParameter("X-messageID", UUID.randomUUID().toString());
		this.getPlatform().getMessenger().deliverMessage(message);
		return this.messageContext.addSentMessage(message);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends MessageInterface> MessageLog sendMessage(AgentID receiver, T message)
			throws MessageReceiverNotFoundException, PlatformNotFoundException {
		message.addUserDefinedParameter("X-messageID", UUID.randomUUID().toString());
		this.getPlatform().getMessenger().deliverMessage(receiver, message);
		return this.messageContext.addSentMessage(message);
	}
	
	public synchronized List<MessageInterface> getAllMessages() {
		List<MessageInterface> messages=new ArrayList<>();
		while (!this.messageQueue.isEmpty()) {
			messages.add(this.messageQueue.remove());
		}
		return messages;
	}
	
	public List<MessageInterface> peekAllMessages() {
		return new ArrayList<>(messageQueue);
	}
	
	@Override
	public void forceStop() {
		xForceStop(); //TODO(rbu) <- Two different forceStops, refactor!
		
		// The messenger should also be a death listener 
		synchronized(this.deathListeners){
			if(!this.deathListeners.isEmpty()){
				 // TODO: upon executing the following it might be the case that a listener is added whilst the agent is dying. This listener would not be notified.
				for(AgentDeathListener listener : this.deathListeners){
					listener.agentDied(this.getAID());
				}
			} 
		}
	}
	
	/** Add a listener that listens for the death of this agent. */
	public final void registerAgentDeathListener(final AgentDeathListener listener){
		synchronized(this.deathListeners){ 
			this.deathListeners.add(listener);
		}
	}
	
	/** Remove a listener that listens for the death of this agent. */
	public final void deregistredDeathListener(final AgentDeathListener listener){
		synchronized(this.deathListeners){
			this.deathListeners.remove(listener);
		}
	}
	
	/**
	 * Execute a given plan. This method will first check whether the plan has a goal 
	 * and if so, whether that goal is still relevant. In case the plan has a goal and the 
	 * goal is not relevant anymore (because it is not in the list of current goals anymore) 
	 * then the plan will not be executed.
	 */
	public final T executePlan(final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T> plan) throws nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError {
		if(plan.goalIsRelevant(this.planInterface))
			return plan.execute(this.planInterface);

		return null;
	} 
	
	// vvv vvv vvv
	
	/**
	 * If the agent receives input then this method will be called upon to check 
	 * whether it is required to reschedule. If so (i.e. when the agent is currently 
	 * sleeping) then the agent's runnable will be rescheduled by this method.  
	 */
	private void checkWhetherToReschedule(){
		if(this.rescheduler == null){
				throw new IllegalStateException("No selfrescheduler set for AgentRuntimeData");
		}
		if (!this.State.isActive()) {
				this.State = FIPAAgentState.ACTIVE;
				this.rescheduler.wakeUp();
				Platform.getLogger().log(Agent.class, Level.FINER, "Agent " + getAID().getName() + " woken up");
		}
    }
		
	//////////////////////////////////////////
	//// EXTERNAL INTERFACE FUNCTIONALITY ////
	////////////////////////////////////////// 

	/** Put an external event in this agent. Will be processed the next deliberation cycle. */
	public final void addExternalTrigger(final Trigger trigger){
		synchronized(this.externalTriggers){ 
			this.externalTriggers.add(trigger);
			checkWhetherToReschedule();
		}
	}

	//////////////////////////////////////
	//// PLAN INTERFACE FUNCTIONALITY ////
	//////////////////////////////////////  
	
	/**
	 * Obtain the context that belongs to a given class.
	 * @param klass Class type of required context
	 * @return The context class, or <code>null</code> if not present
	 */
	public final <C extends Context> C getContext(final Class<C> klass) { //throws IllegalArgumentException {
		return this.contextContainer.getContext(klass);
	}
	
	public final void addContext(final Context context) {
		contextContainer.addContext(context);
	}
	
	public final Collection<Context> getAllContexts() {
		return contextContainer.getAllContext();
	}
	
	 // No synchronize on goals as maximally 1 thread at a time can call these methods
	
	/** Check whether the list of current goals contains the provided argument goal. */
	public final boolean hasGoal(final Goal goal){
		return this.goals.contains(goal);
	}
	
	/** Remove the provided goal from the list of current goals. */
	public final void dropGoal(final Goal goal){
	    synchronized (this.goals) {
            this.goals.remove(goal);
        }
	}
	
	/** Add a goal to the list of current goals. Will check whether the list of 
	 * current goals already contains the provided goal. */
	public final void adoptGoal(final Goal goal){
	    synchronized (this.goals) {
            if (!hasGoal(goal)) {
                this.goals.add(goal);
            }
        }
	}

	/** Add a plan to the list of current plans. This plan will be executed during
	 * the next "execute plans" deliberation step. */
	public final void adoptPlan(final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T> plan){
		synchronized(this.plans){
			this.plans.add(plan);
		}
	}
	
	/** Add a plan to the list of current plans. This plan will be executed during
	 * the next "execute plans" deliberation step. The asynchronous version of adopt plan 
	 * can be used to adopt a plan if the agent is possibly sleeping, as it check whether
	 *  to reschedule the agent for execution. */
	public final void asynchronousAdoptPlan(final nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T> plan){
		adoptPlan(plan);
		checkWhetherToReschedule();
	}

	/** Add an interceptor for goals. */
	public final void adoptGoalInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		if(interceptor.isTriggerConsuming()) this.goalInterceptors.add(interceptor);
		else this.goalInterceptors.add(0,interceptor);
	}
	
	/** Add an interceptor for external triggers. */
	public final void adoptExternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		if(interceptor.isTriggerConsuming()) this.externalTriggerInterceptors.add(interceptor);
		else this.externalTriggerInterceptors.add(0,interceptor);
	}

	/** Add an interceptor for internal triggers. */
	public final void adoptInternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		if(interceptor.isTriggerConsuming()) this.internalTriggerInterceptors.add(interceptor);
		else this.internalTriggerInterceptors.add(0,interceptor);
	}
	
	/** Add an interceptor for messages. */
	public final void adoptMessageInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		if(interceptor.isTriggerConsuming()) this.messageInterceptors.add(interceptor);
		else this.messageInterceptors.add(0,interceptor);
	}
	
	/** Add an internal trigger to the list of current internal triggers. This trigger 
	 * will be processed during the next deliberation cycle.*/
	public final void addInternalTrigger(final Trigger trigger){
		synchronized (this.internalTriggers) {
			this.internalTriggers.add(trigger); 
			this.checkWhetherToReschedule(); 
		}
	} 

	/**
	 * By default an agent is never finished, unless this method is called explicitly
	 * from within a plan. If this method is called then the agent will be killed and 
	 * removed from the platform before it can start a new deliberation cycle.
	 */
	public final void finished(){
		this.finished = true;
	}
	 
	/////////////////////////////////////////
	//// DELIBERATION STEP FUNCTIONALITY ////
	/////////////////////////////////////////
		
	/** Obtain and remove the current external triggers. This will return a new 
	 * list of triggers. */
	public final List<Trigger> getAndRemoveExternalTriggers(){ 
		synchronized(this.externalTriggers){
			if(this.externalTriggers.isEmpty()) return Collections.emptyList();
			else {
				List<Trigger> snapshot = new ArrayList<>(this.externalTriggers);
				this.externalTriggers.clear();
				return snapshot;
			} 
			// TODO: if I used snapshot = this.externalTriggers; this.externalTriggers = new ArrayList<>(); then some triggers were lost.
			// 		 Is this because the lock is bound to address that this.externaltriggers points to, and not to the field this.externalTriggers?
		}
	}
	// get internal triggers, no need to synchronize as only the deliberation thread 
	// can add new internal triggers, which is the same thread as the one that calls this method.
	/** Obtain and remove the current internal triggers. This will return a new 
	 * listof triggers. */
	public final List<Trigger> getAndRemoveInternalTriggers(){
		if(this.internalTriggers.isEmpty()) return Collections.emptyList();
		else {
			List<Trigger> snapshot = new ArrayList<>(this.internalTriggers);
			this.internalTriggers.clear();
			return snapshot;
		}
	}
	
	// get goals, returns new list as it should not be possible to add goals outside of adopt goal (similar for dropgoal)
	/** Obtain new list that contains the current goals. Manipulating the returned list 
	 * will not add/remove goals to the agent. The goals itself though are not cloned. */
	public final List<Goal> getGoals(){
		synchronized (this.goals) {
			if (this.goals.isEmpty()) return Collections.emptyList();
			else return new ArrayList<>(this.goals);
		}
	}
	
	/** Remove all goals that are achieved given the contexts of the agent. */
	public final void clearAchievedGoals(){
		synchronized (this.goals) {
			if (!this.goals.isEmpty()) {
				List<Goal> snapshot = new ArrayList<>(this.goals);
				for (Goal goal : snapshot) {
					if (goal != null && goal.isAchieved(this.contextInterface)) {
						this.goals.remove(goal);
					}
				}
			}
		}
	}
	
	/** Get the plan scheme base. */
	public nl.uu.cs.iss.ga.sim2apl.core.plan.PlanSchemeBase<T> getPlanSchemeBase(){
		return this.planSchemeBase;
	}

	/** Get the goal plan schemes of the plan scheme base. */
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme<T>> getGoalPlanSchemes(){
		return this.planSchemeBase.getGoalPlanSchemes();
	}

	/** Get the external trigger plan schemes of the plan scheme base. */
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme<T>> getExternalTriggerPlanSchemes(){
		return this.planSchemeBase.getExternalTriggerPlanSchemes();
	}
	
	/** Get the internal trigger plan schemes of the plan scheme base. */
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme<T>> getInternalTriggerPlanSchemes(){
		return this.planSchemeBase.getInternalTriggerPlanSchemes();
	}

	/** Get the message plan schemes of the plan scheme base. */
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme<T>> getMessagePlanSchemes(){
		return this.planSchemeBase.getMessagePlanSchemes();
	}

	/** Get the goal interceptors. */
	public final Iterator<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> getGoalInterceptors(){
		return this.goalInterceptors.iterator();
	}
	
	/** Get the external trigger interceptors. */
	public final Iterator<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> getExternalTriggerInterceptors(){
		return this.externalTriggerInterceptors.iterator();
	}
	
	/** Get the internal trigger interceptors. */
	public final Iterator<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> getInternalTriggerInterceptors(){
		return this.internalTriggerInterceptors.iterator();
	}
	
	/** Get the message interceptors. */
	public final Iterator<nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T>> getMessageInterceptors(){
		return this.messageInterceptors.iterator();
	}

	/** Remove a goal interceptor. */
	public final void removeGoalInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.goalInterceptors.remove(interceptor);
	}
	
	/** Remove an external trigger interceptor. */
	public final void removeExternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.externalTriggerInterceptors.remove(interceptor);
	}
	
	/** Remove an internal trigger interceptor. */
	public final void removeInternalTriggerInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.internalTriggerInterceptors.remove(interceptor);
	}
	
	/** Remove a message interceptor. */
	public final void removeMessageInterceptor(final nl.uu.cs.iss.ga.sim2apl.core.plan.TriggerInterceptor<T> interceptor){
		this.messageInterceptors.remove(interceptor);
	}
	
	/**
	 * Try to apply for a given trigger a given plan scheme. If the plan scheme can 
	 * instantiate given the trigger and the current contexts of the agent, then the 
	 * plan will be inserted into the list of current plans. 
	 * @param trigger Trigger that may trigger the plan scheme. 
	 * @param planScheme Plan scheme to try out.
	 * @return True iff the plan scheme was instantiated. 
	 */
	public final boolean tryApplication(final Trigger trigger, final nl.uu.cs.iss.ga.sim2apl.core.plan.PlanScheme<T> planScheme){
		Plan<T> result = planScheme.instantiate(trigger, this.contextInterface);
		if(result != null && !result.equals(Plan.UNINSTANTIATED())){
			adoptPlan(result);
			return true;
		} else return false;
	}

	/** Get a new list with the current instantiated plans of the agent.	 */
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T>> getPlans(){
		synchronized(this.plans){
			if(this.plans.isEmpty()) return Collections.emptyList();
			else return new ArrayList<>(this.plans); 
		}
	}
	
	public final List<nl.uu.cs.iss.ga.sim2apl.core.plan.Plan<T>> getShutdownPlans(){
		synchronized(this.downPlans){
			if(this.downPlans.isEmpty()) return Collections.emptyList();
			else return new ArrayList<>(this.downPlans); 
		}
	}

	/** Remove a plan from the list of current plans. */
	public final void removePlan(final Plan<T> plan){
		synchronized(this.plans){
			this.plans.remove(plan);
		}
	}

	///////////////////////////////////
	//// KILL SWITCH FUNCTIONALITY ////
	///////////////////////////////////
	/** This will forcibly kill the agent. It may finish its current deliberation cycle
	 * if applicable. It will be killed an removed from the agent platform before the next
	 * cycle executes. All death listeners will be notified.  */
	public final void xForceStop(){
		this.forciblyStop = true;
	} 
	 
	/////////////////////////////////////////////
	//// DELIBERATION RUNNABLE FUNCTIONALITY ////
	/////////////////////////////////////////////
	/**
	 * Check whether the agent is done with execution.
	 * @return True iff the agent is forcibly stopped or is finished. 
	 */
	public final boolean isDone(){
		return this.forciblyStop || this.finished;
	}
	
	public final void setSelfRescheduler(final SelfRescheduler rescheduler){
		this.rescheduler = rescheduler;
	}
	
	/**
	 * A check to determine whether the agent should go to sleep.
	 * @return True iff the agent is already sleeping or there are no current plans and triggers.
	 * 
	 * Sleeping should change to state is waiting  (Mohammad) 
	 * 
	 */
	public final boolean checkSleeping(){
		synchronized (this.externalTriggers) {
			synchronized(this.internalTriggers){
				synchronized (this.goals) {
					synchronized (this.plans) {
						if (!this.State.isActive()) return true;
						else if (this.plans.size() == 0 &&
								this.externalTriggers.size() == 0 &&
								this.internalTriggers.size() == 0 &&
								this.goals.size() == 0 &&
								this.messageQueue.peek() == null
						) {
							this.State = FIPAAgentState.WAITING;
						}
						return !this.State.isActive();
					}
				}
			}
		}
	}
	 
	
	/** Obtain the agent's deliberation cycle. */
	public final List<DeliberationStep> getSenseReasonCycle(){
		return this.senseReasonCycle;
	}

	/** Obtain the act part of the deliberation cycle. THis is the only part of the cycle that is
	 * allowed to produce actions */
	public final List<DeliberationActionStep<T>> getActCycle() { return this.actCycle; }

	public Platform getPlatform() throws PlatformNotFoundException{
		if(planInterface==null) {
			throw new PlatformNotFoundException("Platform is null");
		}
		return platform;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}
}
