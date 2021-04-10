package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationResult;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
    List<Future<DeliberationResult<T>>> doTick();

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
     * Use the multi-threaded approach used by this tick executor for agent deliberation to execute a collection of
     * callables. Useful for extending the multi-threaded approach to scenario's outside of agent deliberation
     *
     * @param tasks     A collection of tasks to execute in parallel
     * @param <X>       Generic return type of the callables in the tasks collection
     * @return          A list of futures with the results of the executed tasks (with the order preserved)
     * @throws InterruptedException If the tasks could not be scheduled correctly.
     */
    <X> List<Future<X>> useExecutorForTasks(Collection<? extends Callable<X>> tasks) throws InterruptedException;

    /**
     * Shuts down this executor and cleans up
     */
    void shutdown();
}
