package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;

import java.util.*;
import java.util.concurrent.*;

/**
 * A default time step executor that uses a ThreadPoolExecutor to run the agents when the tick needs
 * to be performed.
 */
public class DefaultBlockingTickExecutor<T> implements TickExecutor<T> {

    /** Internal counters **/
    private int tick = 0;
    private int stepDuration;

    /**
     * A random object, which can be used to have agent execution occur in deterministic manner
     */
    private Random random;

    /** The ExecutorService that will be used to execute one sense-reason-act step for all scheduled agents **/
    private final ExecutorService executor;

    /** The list of agents scheduled for the next tick **/
    private final Map<AgentID, DeliberationRunnable<T>> scheduledRunnables;

    /**
     * Default constructor
     * @param nThreads Number of threads to use to execute the agent's sense-reason-act cycles.
     */
    public DefaultBlockingTickExecutor(int nThreads) {
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.scheduledRunnables = new ConcurrentHashMap<>();
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
    public DefaultBlockingTickExecutor(int nThreads, Random random) {
        this(nThreads);
        this.random = random;
    }

    public <X> List<Future<X>> useExecutorForTasks(Collection<? extends Callable<X>> tasks) throws InterruptedException {
        return this.executor.invokeAll(tasks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean scheduleForNextTick(DeliberationRunnable<T> agentDeliberationRunnable) {
        this.scheduledRunnables.put(agentDeliberationRunnable.getAgentID(), agentDeliberationRunnable);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMap<AgentID, List<T>> doTick() {
        ArrayList<DeliberationRunnable<T>> runnables;
        // TODO make sure running can only happen once with some sort of mutex? How to verify if a tick is currently being executed?
        synchronized (this.scheduledRunnables) {
            runnables = new ArrayList<>(this.scheduledRunnables.values());
            this.scheduledRunnables.clear();
        }

        if(this.random != null) {
            runnables.sort(Comparator.comparing(deliberationRunnable -> deliberationRunnable.getAgentID().getUuID()));
            Collections.shuffle(runnables, this.random);
        }

        HashMap<AgentID, List<T>> agentPlanActions = new HashMap<>();

        long startTime = System.currentTimeMillis();
        try {
            List<Future<List<T>>> currentAgentFutures = this.executor.invokeAll(runnables);
            for(int i = 0; i < currentAgentFutures.size(); i++) {
                agentPlanActions.put(runnables.get(i).getAgentID(), currentAgentFutures.get(i).get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        this.stepDuration = (int) (System.currentTimeMillis() - startTime);

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
            return new ArrayList<>(this.scheduledRunnables.keySet());
        }
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
