package nl.uu.cs.iss.ga.sim2apl.core.deliberation;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;

import java.util.List;

public class DeliberationResult<T> {

    private final AgentID agentID;
    private final List<T> actions;

    public DeliberationResult(AgentID agentID, List<T> actions) {
        this.agentID = agentID;
        this.actions = actions;
    }

    public AgentID getAgentID() {
        return agentID;
    }

    public List<T> getActions() {
        return actions;
    }
}
