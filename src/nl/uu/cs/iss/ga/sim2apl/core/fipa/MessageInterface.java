package nl.uu.cs.iss.ga.sim2apl.core.fipa;

import java.util.Collection;

import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;

public interface MessageInterface extends Trigger {
	
	Collection<nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID> getReceiver();

	AgentID getSender();
	
	void addUserDefinedParameter(String key, String value);
	
	String getUserDefinedParameter(String key);
	
	String getContent();
}
