package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;

import java.util.HashMap;
import java.util.List;

public interface TickHookProcessor<T> {

    void tickPreHook(long startingTick);

    void tickPostHook(long finishedTick, int tickDuration, HashMap<AgentID, List<T>> producedAgentActions);

    void simulationFinishedHook(long lastTick, int lastTickDuration);
}
