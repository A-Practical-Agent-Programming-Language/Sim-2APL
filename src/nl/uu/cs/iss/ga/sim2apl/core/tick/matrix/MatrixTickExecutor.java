package nl.uu.cs.iss.ga.sim2apl.core.tick.matrix;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;
import nl.uu.cs.iss.ga.sim2apl.core.tick.TickExecutor;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * A default time step executor that uses a ThreadPoolExecutor to run the agents when the tick needs
 * to be performed.
 */
public class MatrixTickExecutor<T> implements TickExecutor<T> {
    private static final Logger LOG = Logger.getLogger(MatrixTickExecutor.class.getName());
    
    public final String CONTROLLER_ADDRESS = "127.0.0.1";
    public final int CONTROLLER_PORT = 16001;

    /** Internal counters **/
    private int tick = 0;
    private int stepDuration = -1;

    /**
     * A random object, which can be used to have agent execution occur in deterministic manner
     */
    private Random random;

    /** The ExecutorService that will be used to execute one sense-reason-act step for all scheduled agents **/
    private final ExecutorService executor;

    /** The list of agents scheduled for the next tick **/
    private final ArrayList<DeliberationRunnable<T>> scheduledRunnables;
    
    private final MatrixAgentThread<T> agentThread;
    private final MatrixStoreThread<T> storeThread;
    private boolean finished = false;

    /**
     * Default constructor
     * @param nThreads Number of threads to use to execute the agent's sense-reason-act cycles.
     */
    public MatrixTickExecutor(int nThreads) {
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.scheduledRunnables = new ArrayList<>();
        
        this.agentThread = new MatrixAgentThread<>(0, CONTROLLER_ADDRESS, CONTROLLER_PORT, this.executor);
        this.storeThread = new MatrixStoreThread<>(0, CONTROLLER_ADDRESS, CONTROLLER_PORT);
    }

    /**
     * Constructor that allows setting a (seeded) random, for ordering deliberation cycles
     * before each tick.
     *
     * <b>NOTICE:</b> when the number of threads is larger then 1, some variation in order of
     * agent execution may still occur. If agents use the same random object for selecting actions,
     * the nextInt they receive may no longer be deterministic
     * @param nThreads  Number of threads to use to execute the agent's sense-reason-act cycles.
     * @param random    A (seeded) random object
     */
    public MatrixTickExecutor(int nThreads, Random random) {
        this(nThreads);
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean scheduleForNextTick(DeliberationRunnable<T> agentDeliberationRunnable) {
        if (!this.scheduledRunnables.contains(agentDeliberationRunnable)) {
            this.scheduledRunnables.add(agentDeliberationRunnable);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMap<AgentID, List<T>> doTick() {
        if (finished) {
            LOG.severe("Simulation already finished");
            throw new RuntimeException("Simulation already finished");
        }
        
        ArrayList<DeliberationRunnable<T>> runnables;
        // TODO make sure running can only happen once with some sort of mutex? How to verify if a tick is currently being executed?
        synchronized (this.scheduledRunnables) {
            runnables = new ArrayList<>(this.scheduledRunnables);
            this.scheduledRunnables.clear();
        }

        if(this.random != null) {
            runnables.sort(Comparator.comparing(deliberationRunnable -> deliberationRunnable.getAgentID().getUuID()));
            Collections.shuffle(runnables, this.random);
        }
        
        try {
            LOG.info("Sending runnables to agent thread");
            this.agentThread.inq.put(runnables);
        } catch (InterruptedException ex) {
            LOG.severe("Interrupted while sending runnables to agent thread: " + ex.toString());
            throw new RuntimeException("Interrupted while sending runnables to agent thread: " + ex.toString());
        }
        HashMap<AgentID, List<T>> agentPlanActions = null;
        try {
            LOG.info("Waiting for ");
            agentPlanActions = this.storeThread.outq.take();
            if (agentPlanActions == null) {
                finished = true;
            }
        } catch (InterruptedException ex) {
            LOG.severe("Interrupted while sending runnables to agent thread: " + ex.toString());
            throw new RuntimeException("Interrupted while sending runnables to agent thread: " + ex.toString());
        }

        tick++;
        return agentPlanActions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentTick() {
        return this.tick;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLastTickDuration() {
        return this.stepDuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AgentID> getScheduledAgents() {
        List<AgentID> scheduledAgents = new ArrayList<>();
        synchronized (this.scheduledRunnables) {
            for(DeliberationRunnable runnable : this.scheduledRunnables) {
                scheduledAgents.add(runnable.getAgentID());
            }
        }
        return scheduledAgents;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNofScheduledAgents() {
        synchronized (this.scheduledRunnables) {
            return this.scheduledRunnables.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        this.executor.shutdown();
    }
}
