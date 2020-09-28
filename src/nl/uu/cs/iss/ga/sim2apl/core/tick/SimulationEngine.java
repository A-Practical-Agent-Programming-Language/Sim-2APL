package nl.uu.cs.iss.ga.sim2apl.core.tick;

public interface SimulationEngine<T> {

    /**
     * This method starts the simulation.
     * <p>
     * This method should be implemented in such a way that before a new tick is
     * started, the preTickHook methods of all registered TickHookProcessors are
     * allowed to run in a blocking manner, and after the tick is finished the
     * postTickHook is allowed to run in a blocking manner.
     * <p>
     * When the simulation is finished, this method should allow the
     * simulationFinishedHook to run in a blocking manner.
     *
     * @return True after the entire simulation is finished
     */
    boolean start();

    /**
     * Registers a new tick hook processor. Each of these processors will
     * be allowed to perform their pre and post tick hooks before a new
     * tick will be started
     *
     * @param processor TickHookProcessor to register
     */
    void registerTickHookProcessor(TickHookProcessor<T> processor);

    /**
     * Deregisters a tick hook processor from being notified of pre and
     * post tick hooks
     *
     * @param processor TickHookProcessor to deregister
     */
    void deregisterTickHookProcessor(TickHookProcessor<T> processor);
}
