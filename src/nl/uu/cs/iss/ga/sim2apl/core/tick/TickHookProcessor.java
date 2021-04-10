package nl.uu.cs.iss.ga.sim2apl.core.tick;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.deliberation.DeliberationResult;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

public interface TickHookProcessor<T> {

    void tickPreHook(long startingTick);

    void tickPostHook(long finishedTick, int tickDuration, List<Future<DeliberationResult<T>>> producedAgentActions);

    void simulationFinishedHook(long lastTick, int lastTickDuration);
}
