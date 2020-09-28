// RPC Proxy class for communicating with the Matrix
package nl.uu.cs.iss.ga.sim2apl.core.tick.matrix;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Logger;
import org.javatuples.Pair;

/**
 * RPCProxy provides a simple JSONRPC over TCP/IP connection service.
 *
 * This is how the program connects to the Matrix.
 */
public class MatrixRPCProxy
{
    private static final Logger LOG = Logger.getLogger(MatrixRPCProxy.class.getName());

    // initialize socket and input output streams
    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    // constructor to put ip address and port
    public MatrixRPCProxy(String address, int port)
    {
        // establish a connection
        try
        {
            socket = new Socket(address, port);
            LOG.info(String.format("Connected to %s:%d", address, port));

	    reader = new BufferedReader(
                    new InputStreamReader(
                        socket.getInputStream(),
                        Charset.forName("US-ASCII")
                        )
                    );

            writer = new BufferedWriter(
                    new OutputStreamWriter(
                        socket.getOutputStream(),
                        Charset.forName("US-ASCII")
                        )
                    );
	}
        catch(UnknownHostException u)
	{
            LOG.severe(u.toString());
            throw new RuntimeException("Error connecting to controller");
	}
        catch(IOException e)
	{
            LOG.severe(e.toString());
            throw new RuntimeException("Error connecting to controller");
	}
    }


    public void close() throws IOException
    {
        writer.close();
        reader.close();
        socket.close();
    }

    public JsonElement call(String method, JsonObject params)
    {
        JsonObject iobj = new JsonObject();
        iobj.addProperty("jsonrpc", "2.0");
        iobj.addProperty("id", UUID.randomUUID().toString());
        iobj.addProperty("method", method);
        iobj.add("params", params);

        Gson gson = new Gson();
        String ijson = gson.toJson(iobj);
        ijson = ijson + "\n"; // NOTE: The newline is important
        try {
            writer.write(ijson);
            writer.flush();
        } catch (IOException ex) {
            LOG.severe(ex.toString());
            throw new RuntimeException("Error Sending RPC Request");
        }

        String ojson;
        try {
            ojson = reader.readLine();
        } catch (IOException ex) {
            LOG.severe(ex.toString());
            throw new RuntimeException("Error Receiving RPC Response");
        }
        JsonObject oobj = JsonParser.parseString(ojson).getAsJsonObject();

        if (!oobj.has("jsonrpc") || !"2.0".equals(oobj.get("jsonrpc").getAsString())) {
            throw new RuntimeException("Invalid RPC Response: " + ojson);
        }
        if (oobj.has("error")) {
            throw new RuntimeException("RPC Exception: " + ojson);
        }

        return oobj.get("result");
    }

    public int get_agentproc_seed(int agentproc_id) {
        JsonObject params = new JsonObject();
        params.addProperty("agentproc_id", agentproc_id);
        JsonElement response = this.call("get_agentproc_seed", params);
        int seed = response.getAsInt();

        LOG.info(String.format("Agent %d: Got seed: %d", agentproc_id, seed));
        return seed;
    }

    public int can_we_start_yet(int agentproc_id) {
        JsonObject params = new JsonObject();
        params.addProperty("agentproc_id", agentproc_id);
        JsonElement response = this.call("can_we_start_yet", params);
        int cur_round = response.getAsJsonObject().get("cur_round").getAsInt();

        LOG.info(String.format("Agent %d: starting round %d", agentproc_id, cur_round));
        return cur_round;
    }

    public void register_events(int agentproc_id, JsonArray events) {
        JsonObject params = new JsonObject();
        params.addProperty("agentproc_id", agentproc_id);
        params.add("events", events);
        this.call("register_events", params);

        LOG.info(String.format("Agent %d, sent %d events", agentproc_id, events.size()));
    }

    public Pair<String, JsonArray> get_events(int storeproc_id) {
        JsonObject params = new JsonObject();
        params.addProperty("storeproc_id", storeproc_id);
        JsonElement response = this.call("get_events", params);

        String code = response.getAsJsonObject().get("code").getAsString();
        if ("EVENTS".equals(code)) {
            JsonArray updates = response.getAsJsonObject().get("events").getAsJsonArray();
            LOG.info(String.format("Store %d: Received %d updates", storeproc_id, updates.size()));
            return new Pair<>(code, updates);
        } else  if ("FLUSH".equals(code)) {
            LOG.info(String.format("Store %d: Received flush", storeproc_id));
            return new Pair<>(code, null);
        } else if ("SIMEND".equals(code)) {
            LOG.info(String.format("Store %d: Received simend", storeproc_id));
            return new Pair<>(code, null);
        } else {
            throw new RuntimeException(String.format("Received unknown code: %s", code));
        }
    }
}
