package nl.uu.cs.iss.ga.sim2apl.core.tick.matrix;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationResult;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationRunnable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * An Agent Thread is responsible for producing events on behalf of a specific set of agents.
 */
public class MatrixAgentThread<T> implements Runnable {
    private static final Logger LOG = Logger.getLogger(MatrixAgentThread.class.getName());
    
    private int agentproc_id = -1;
    private MatrixRPCProxy proxy = null;
    
    public Random random = null;
    public int cur_round = -1;
    
    BlockingQueue<List<DeliberationRunnable<T>>> inq = null;
    private ExecutorService executor = null;
    private Thread thread = null;
    
    MatrixAgentThread(int agentproc_id, String address, int port, ExecutorService executor) {
        LOG.info(String.format("Creating agent thread: %d", agentproc_id));

        this.agentproc_id = agentproc_id;
        this.proxy = new MatrixRPCProxy(address, port);
        this.random = new Random();
        this.inq = new LinkedBlockingQueue<>(1);
        this.executor = executor;

        int seed = this.proxy.get_agentproc_seed(agentproc_id);
        this.random.setSeed(seed);

        this.thread = new Thread(this);
        this.thread.start();
    }
    
    @Override
    public void run() {
        Gson gson = new Gson();
        Type arrayListStringType = new TypeToken<ArrayList<String>>(){}.getType();
        
        try {
            while (true) {
                cur_round = this.proxy.can_we_start_yet(agentproc_id);
                LOG.info(String.format("Agent %d received round %d", agentproc_id, cur_round));
                if (cur_round == -1) {
                    LOG.info(String.format("Agent thread %d: stopping", agentproc_id));
                    return;
                }
                
                long startTime = System.currentTimeMillis();
                
                List<DeliberationRunnable<T>> runnables = inq.take();
                List<Future<DeliberationResult<T>>> agentActionFutures = this.executor.invokeAll(runnables);

                try {
                    for(Future<DeliberationResult<T>> resultFuture : agentActionFutures) {
                        DeliberationResult<T> result = resultFuture.get();
                        List<String> agentActions = result.getActions().stream().filter(Objects::nonNull).map(gson::toJson).collect(Collectors.toList());
                        JsonObject update = new JsonObject();
                        update.addProperty("agentID", result.getAgentID().toString());
                        update.add("actions", gson.toJsonTree(agentActions, arrayListStringType));
                        JsonArray updates = new JsonArray();
                        updates.add(update);
                        this.proxy.register_events(agentproc_id, updates);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.severe("Error running runnable: " + ex.toString());
                    ex.printStackTrace();
                }
                long stepDuration = (System.currentTimeMillis() - startTime);
                LOG.info(String.format("Agent thread %d: Round %d: Event production took %d ms", agentproc_id, cur_round, stepDuration));
            }
        } catch (InterruptedException ex) {
            LOG.severe("Got Interrupted:" + ex.toString());
            throw new RuntimeException("Got Interrupted: " + ex.toString());
        }
    }
}
