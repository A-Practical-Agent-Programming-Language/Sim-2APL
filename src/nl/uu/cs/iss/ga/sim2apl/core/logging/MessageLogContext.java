package nl.uu.cs.iss.ga.sim2apl.core.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.SubmissionPublisher;

import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageInterface;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageLog;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Context;

public class MessageLogContext extends SubmissionPublisher<MessageLog> implements Context {

	/** The sendMessageHistory contains the history of messages sent by the Agent */
	private final SortedSet<MessageLog> sendMessageHistory = new ConcurrentSkipListSet<>();
	private final Map<UUID, MessageLog> sendMessageMap = new ConcurrentHashMap<>();

	/**
	 * The sendMessageHistory contains the history of messages received by the Agent
	 */
	private final SortedSet<MessageLog> receiveMessageHistory = new ConcurrentSkipListSet<>();
	private final Map<UUID, MessageLog> receiveMessageMap = new ConcurrentHashMap<>();

	public MessageLog addSentMessage(MessageInterface message) {
		final MessageLog log = new MessageLog(message, false);
		this.sendMessageHistory.add(log);
		this.sendMessageMap.put(log.getID(), log);
		this.submit(log);
		return log;
	}
	
	public MessageLog addReceivedMessage(MessageInterface message) {
		final MessageLog log = new MessageLog(message, true);
		this.receiveMessageHistory.add(log);
		this.receiveMessageMap.put(log.getID(), log);
		this.submit(log);
		return log;
	}

	public MessageLog getReceivedMessageLog(UUID messageID) {
		return this.receiveMessageMap.get(messageID);
	}
	
	public MessageLog getSentMessageLog(UUID messageID) {
		return this.sendMessageMap.get(messageID);
	}
	
	public SortedSet<MessageLog> getMessageHistory() {
		ConcurrentSkipListSet<MessageLog> history = new ConcurrentSkipListSet<MessageLog>();
		history.addAll(this.receiveMessageHistory);
		history.addAll(this.sendMessageHistory);
		return history.headSet(new MessageLog());
	}
	public SortedSet<MessageLog> getSentMessageHistory(){
		return this.sendMessageHistory.headSet(new MessageLog());
	}
	public SortedSet<MessageLog> getReceiveMessageHistory(){
		return this.receiveMessageHistory.headSet(new MessageLog());
	}
}
