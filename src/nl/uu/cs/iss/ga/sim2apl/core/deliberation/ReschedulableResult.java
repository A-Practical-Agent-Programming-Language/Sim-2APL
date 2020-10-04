package nl.uu.cs.iss.ga.sim2apl.core.deliberation;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;

import java.util.List;

public class ReschedulableResult<T> {

    final AgentID agentID;
    final DeliberationRunnable<T> deliberationRunnable;
    final boolean reschedule;
    final List<T> result;

    public ReschedulableResult(DeliberationRunnable<T> deliberationRunnable, List<T> result, boolean reschedule) {
        this.deliberationRunnable = deliberationRunnable;
        this.agentID = deliberationRunnable.getAgentID();
        this.reschedule = reschedule;
        this.result = result;
    }

    public AgentID getAgentID() {
        return agentID;
    }

    public DeliberationRunnable<T> getDeliberationRunnable() {
        return deliberationRunnable;
    }

    public boolean isReschedule() {
        return reschedule;
    }

    public List<T> getResult() {
        return result;
    }
}
