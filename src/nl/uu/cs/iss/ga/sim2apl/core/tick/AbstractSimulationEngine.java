package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.platform.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * An abstract simulation engine, that only leaves the initiation of a tick
 * to the programmer.
 *
 * Before and after each tick, as well as at the end of the simulation, the
 * appropriate tickHooks need to be called.
 */
public abstract class AbstractSimulationEngine<T> implements SimulationEngine<T> {

    protected List<TickHookProcessor<T>> tickHookProcessorList;
    protected int nIterations;
    protected final Platform platform;

    /**
     * Instantiate this class with only a platform
     * @param platform  Platform
     */
    public AbstractSimulationEngine(Platform platform) {
        this.tickHookProcessorList = new ArrayList<>();
        this.platform = platform;
        this.nIterations = -1;
    }

    /**
     * Instantiate this class with a platform and an initial (set of) TickHookProcessor(s)
     * @param platform      Platform
     * @param processors    TickHookProcessors
     */
    public AbstractSimulationEngine(Platform platform, TickHookProcessor<T>... processors) {
        this(platform);
        this.tickHookProcessorList.addAll(Arrays.asList(processors));
    }

    /**
     * Instantiate this class with a platform and with a specification of how many ticks to run
     *
     * @param platform      Platform
     * @param iterations    Number of ticks to run
     */
    public AbstractSimulationEngine(Platform platform, int iterations) {
        this(platform);
        this.nIterations = iterations;
    }

    /**
     * Instantiate this class with a platform, a specification of how many ticks to run, and
     * an initial (set of) TickHookProcessor(s)
     * @param platform      Platform
     * @param iterations    Number of ticks to run
     * @param processors    TickHookProcessors
     */
    public AbstractSimulationEngine(Platform platform, int iterations, TickHookProcessor<T>... processors) {
        this(platform, processors);
        this.nIterations = iterations;
    }

    /**
     * Run the preTickHook of all registered TickHookProcessors in a blocking manner
     *
     * @param startingTick  The tick that will be started
     */
    protected void processTickPreHooks(int startingTick) {
        this.tickHookProcessorList.forEach(tph -> tph.tickPreHook(startingTick));
    }

    /**
     * Run the postTickHook of all registered TickHookProcessors in a blocking manner
     *
     * @param finishedTick      The tick that has finished
     * @param lastTickDuration  The duration of the last tick in milliseconds
     * @param actions           The hashmap of agent-actions produced during the last tick
     */
    protected void processTickPostHook(int finishedTick, int lastTickDuration, HashMap<AgentID, List<T>> actions) {
        this.tickHookProcessorList.forEach(tph -> tph.tickPostHook(finishedTick, lastTickDuration, actions));
    }

    /**
     * Run the simulationFinishedHook of all registered TickHookProcessors in a blocking manner
     *
     * @param lastTick          The last executed tick before the simulation finished
     * @param lastTickDuration  The time it took to run the last tick
     */
    protected void processSimulationFinishedHook(int lastTick, int lastTickDuration) {
        this.tickHookProcessorList.forEach(thp -> thp.simulationFinishedHook(lastTick, lastTickDuration));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerTickHookProcessor(TickHookProcessor<T> processor) {
        if(!this.tickHookProcessorList.contains(processor)) {
            this.tickHookProcessorList.add(processor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deregisterTickHookProcessor(TickHookProcessor<T> processor) {
        this.tickHookProcessorList.remove(processor);
    }
}
