package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;

import java.util.HashMap;
import java.util.List;

/**
 * A tick executor handles the execution of agent's sense-reason-act cycles.
 * It waits for an explicit external event before it starts execution of a tick.
 */
public interface TickExecutor<T> {

    /**
     * Schedules the deliberation cycle of an agent for the next tick
     * @param agentDeliberationRunnable Deliberation cycle to schedule
     * @return True if deliberation cycle could be scheduled. This can
     * fail if the deliberation cycle already exists on the queue,
     * in which case this method returns false without rescheduling the
     * deliberation cycle
     */
    boolean scheduleForNextTick(DeliberationRunnable<T> agentDeliberationRunnable);

    /**
     * Performs one tick, executing the sense-reason-act cycles of all agents
     * scheduled for that tick. It collects all the actions produced by the
     * agents in that cycle, and orders them in a HashMap, so actions can be
     * linked to the agent which requested them at all times.
     *
     * @return Hashmap of agent ID's and a list of requested actions for that
     * agent
     */
    HashMap<AgentID, List<T>> doTick();

    /**
     * Obtain the current tick index, indicating how many ticks have already
     * passed in the simulation
     *
     * @return Current tick
     */
    int getCurrentTick();

    /**
     * Verify whether a tick is currently being executed
     * @return True iff a tick is currently being executed
     */
    boolean isRunning();

    /**
     * Get the time it took to perform the sense-reason-act cycles of all scheduled
     * agents during the last tick
     *
     * @return Duration of last tick in milliseconds
     */
    int getLastTickDuration();

    /**
     * Get the list of agents which, thus far, have been scheduled for the next tick
     *
     * @return List of scheduled agents
     */
    List<AgentID> getScheduledAgents();

    /**
     * Get the number of agents which, thus far, have been scheduled for the next tick
     *
     * @return Number of scheduled agents
     */
    int getNofScheduledAgents();

    /**
     * Shuts down this executor and cleans up
     */
    void shutdown();
}
