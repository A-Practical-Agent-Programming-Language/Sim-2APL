package nl.uu.cs.iss.ga.sim2apl.core.tick.matrix;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import org.javatuples.Pair;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * The Store Thread is responsible for maintaining the overall system state.
 */
public class MatrixStoreThread<T> implements Runnable {
    private static final Logger LOG = Logger.getLogger(MatrixStoreThread.class.getName());

    private int storeproc_id = -1;
    private MatrixRPCProxy proxy = null;
    
    BlockingQueue<HashMap<AgentID, List<T>>> outq = null;
    private Thread thread = null;
     
    public MatrixStoreThread(int storeproc_id, String address, int port) {
        LOG.info(String.format("Creating store2 thread: %d", storeproc_id));
        
        this.storeproc_id = storeproc_id;
        this.proxy = new MatrixRPCProxy(address, port);
        
        this.outq = new LinkedBlockingQueue<>(1);
        
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        Type arrayListStringType = new TypeToken<ArrayList<String>>(){}.getType();
        
        HashMap<AgentID, List<T>> agentPlanActions;

        try {
            agentPlanActions = new HashMap<>();
            while (true) {
                Pair<String, JsonArray> code_updates = this.proxy.get_events(storeproc_id);
                String code = code_updates.getValue0();
                if ("EVENTS".equals(code)) {
                    JsonArray updates = code_updates.getValue1();
                    for (JsonElement update_e: updates) {
                        JsonObject update = update_e.getAsJsonObject();
                        AgentID agentID;
                        try {
                            agentID = new AgentID(update.getAsJsonPrimitive("agentID").getAsString());
                        } catch (URISyntaxException ex) {
                            throw new RuntimeException("Couldn't reconstruct agent id: " + ex.toString());
                        }
                        ArrayList<T> actions = gson.fromJson(update.get("actions"), arrayListStringType);

                        agentPlanActions.put(agentID, actions);
                    }
                } else if ("FLUSH".equals(code)) {
                    this.outq.put(agentPlanActions);
                    agentPlanActions = new HashMap<>();
                } else if ("SIMEND".equals(code)) {
                    this.outq.put(agentPlanActions);
                    return;
                } else {
                    throw new RuntimeException(String.format("Received unknown code: %s", code));
                }
            }
        } catch (InterruptedException ex) {
            LOG.severe("Got interrupted " + ex.toString());
        }
    }
}
