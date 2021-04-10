package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationResult;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    private Queue<DeliberationRunnable<T>> scheduledRunnables;

    /**
     * Default constructor
     * @param nThreads Number of threads to use to execute the agent's sense-reason-act cycles.
     */
    public DefaultBlockingTickExecutor(int nThreads) {
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.scheduledRunnables = new ConcurrentLinkedQueue<>();
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

    @Override
    public <X> List<Future<X>> useExecutorForTasks(Collection<? extends Callable<X>> tasks) throws InterruptedException {
        return this.executor.invokeAll(tasks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean scheduleForNextTick(DeliberationRunnable<T> agentDeliberationRunnable) {
        this.scheduledRunnables.add(agentDeliberationRunnable);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Future<DeliberationResult<T>>> doTick() {

        Queue<DeliberationRunnable<T>> runnables = this.scheduledRunnables;
        this.scheduledRunnables = new ConcurrentLinkedQueue<>();

        // TODO sorting now becomes an issue. Does it make sense to sort _before_ execution? WOuld it not make more sense to sort action results, since agents are synchronous anyway?
//        if(this.random != null) {
//            runnables.sort(Comparator.comparing(deliberationRunnable -> deliberationRunnable.getAgentID().getUuID()));
//            Collections.shuffle(runnables, this.random);
//        }

        List<Future<DeliberationResult<T>>> currentAgentFutures = null;

        long startTime = System.currentTimeMillis();
        try {
            currentAgentFutures = this.executor.invokeAll(runnables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//            for(Future<DeliberationResult<T>> resultFuture : currentAgentFutures) {
//                DeliberationResult<T> result = resultFuture.get();
//                agentPlanActions.put(result.getAgentID(), result.getActions().stream().filter(Objects::nonNull).collect(Collectors.toList()));
//            }
        this.stepDuration = (int) (System.currentTimeMillis() - startTime);

        tick++;
        return currentAgentFutures;
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
        return this.scheduledRunnables.stream().map(DeliberationRunnable::getAgentID).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNofScheduledAgents() {
        return this.scheduledRunnables.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        this.executor.shutdown();
    }
}
