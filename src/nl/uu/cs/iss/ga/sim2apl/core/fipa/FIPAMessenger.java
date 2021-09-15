package nl.uu.cs.iss.ga.sim2apl.core.fipa;

import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.defaults.messenger.MessageReceiverNotFoundException;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.acl.ACLMessage;
import nl.uu.cs.iss.ga.sim2apl.core.logging.Loggable;
import nl.uu.cs.iss.ga.sim2apl.core.messaging.Messenger;
import nl.uu.cs.iss.ga.sim2apl.core.platform.Platform;

import java.util.HashMap;
import java.util.Map;

public class FIPAMessenger<T> implements Messenger<ACLMessage, T> {

	private static final Loggable logger = Platform.getLogger();
	
	/** Stores the interfaces to agents to inject messages. */
	private final Map<AgentID, Agent<T>> agents;
	
	public FIPAMessenger() {
		this.agents = new HashMap<>();
	}

	/** Store the agent interface. */
	@Override
	public final void register(final Agent<T> agent) {
		synchronized (this.agents) {
			this.agents.put(agent.getAID(), agent);
		}
	}

	/** Remove the agent interface from the messenger. */
	@Override
	public final void deregister(final AgentID agentID) {
		synchronized (this.agents) {
			this.agents.remove(agentID);
		}
	}

	/**
	 * Grab the agent interface of the receiver and add the message in the receiving
	 * agent.
	 */
	public final void sendMessage(final AgentID receiver, final ACLMessage message)
			throws MessageReceiverNotFoundException {
		
		//final AgentID from = message.getEnvelope().getFrom();
		//final String nick = from.getNickName();
		//nicknames.putIfAbsent(from, nick);
		
		//String receiveNick = nicknames.get(receiver);
		//if (receiveNick != null) {
		//	receiver.setNickName(receiveNick);
		//}
		
		synchronized (this.agents) {
			// System.err.println("MESSAGE " + message.getSender().getLocalName() + " -> " +
			// message.getReceiver().get(0).getLocalName() + ": " +
			// message.getPerformative() + " -- " + message.getContent());
			AgentInterface agent = this.agents.get(receiver);
			if (agent == null) {
				// TODO send message to sender that receiver is unknown instead of exception
				throw new MessageReceiverNotFoundException("Trying to send to non-existent agent " + receiver + ".");
			} else {
				agent.receiveMessage(message);
			}
		}
	}

	public void sendMessage(ACLMessage message) { // throws MessageReceiverNotFoundException {
		if (message.getEnvelope().getCountOfIntendedReceiver() > 0) {
			// while (message.getEnvelope().getAllIntendedReceiver().hasNext()){
			message.getEnvelope().getAllIntendedReceiver().forEachRemaining((receiver) -> {
				try {
					// this.sendMessage(message.getEnvelope().getAllIntendedReceiver().next(),
					// message);
					this.sendMessage(receiver, message);
				} catch (MessageReceiverNotFoundException ex) {
					logger.log(FIPAMessenger.class, ex);
					// TODO: re-throw?
				}
			});
			// }
		} else if (message.getEnvelope().getCountOfTo() > 0) {
			// while (message.getEnvelope().getAllTo().hasNext()){
			message.getEnvelope().getAllTo().forEachRemaining((receiver) -> {
				try {
					// this.sendMessage(message.getEnvelope().getAllTo().next(), message);
					this.sendMessage(receiver, message);
				} catch (MessageReceiverNotFoundException ex) {
					logger.log(FIPAMessenger.class, ex);
					// TODO: re-throw?
				}
			});
			// }
		} else {
			agents.keySet().forEach((agentID) -> {
				try {
					this.sendMessage(agentID, message);
				} catch (MessageReceiverNotFoundException ex) {
					logger.log(FIPAMessenger.class, ex);
					// TODO: re-throw?
				}
			});
		}
	}

	@Override
	public void agentDied(AgentID agentID) {
		this.deregister(agentID);

	}

	@Override
	public void deliverMessage(AgentID receiver, ACLMessage message) throws MessageReceiverNotFoundException {
		this.sendMessage(receiver, message);

	}

	@Override
	public void deliverMessage(ACLMessage message) throws MessageReceiverNotFoundException {
		this.sendMessage(message);
	}

	@Override
	public boolean implementsEncoding() {
		return true;
	}
	
	@Override
	public byte[] encodeMessage(ACLMessage message) throws UnsupportedOperationException {
		return message.encode();
	}

	@Override
	public ACLMessage decodeMessage(byte[] asBytes) throws UnsupportedOperationException {
		return ACLMessage.decode(asBytes);
	}
}
