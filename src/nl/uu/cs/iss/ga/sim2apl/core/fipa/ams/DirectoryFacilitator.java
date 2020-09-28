package nl.uu.cs.iss.ga.sim2apl.core.fipa.ams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentArguments;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentContextInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentCreationFailedException;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.agent.PlanToAgentInterface;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Trigger;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.acl.ACLMessage;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.acl.Performative;
import nl.uu.cs.iss.ga.sim2apl.core.logging.Loggable;
import nl.uu.cs.iss.ga.sim2apl.core.plan.Plan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.PlanExecutionError;
import nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.RunOncePlan;
import nl.uu.cs.iss.ga.sim2apl.core.plan.builtin.SubPlanInterface;
import nl.uu.cs.iss.ga.sim2apl.core.platform.Platform;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope;

public class DirectoryFacilitator extends Agent<Void> {

	private static final Loggable logger = Platform.getLogger();
	
	static public enum RequestType {
		SUBSCRIBER_ADD,
		SUBSCRIBER_REMOVE,
		SERVICE_ADD,
		SERVICE_REMOVE,
		
		KILL_AGENT,
		
		UNKOWN
	}
	
	static private class MessageContent {
		public RequestType requestType = RequestType.UNKOWN;
		public AgentID agentId = null;
		public Set<String> serviceNames = new HashSet<>();
	}
	
	private static AgentArguments<Void> CreateAgentArguments(Set<AgentID> others) {
		final AgentArguments<Void> args = new AgentArguments<Void>();

		args.addContext(new nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext(others));
		args.addInitialPlan(getInitialPlan());
		args.addMessagePlanScheme(DirectoryFacilitator::handleMessageScheme);
		args.addExternalTriggerPlanScheme(DirectoryFacilitator::handleExternalScheme);

		return args;
	}

	private static Plan<Void> getInitialPlan() {
		return new RunOncePlan<>() {
			@Override
			public Void executeOnce(PlanToAgentInterface<Void> planInterface) throws PlanExecutionError {
				try {
					List<Set<AgentID>> agentsLists = new ArrayList<>();
					agentsLists.add(planInterface.getAgent().getPlatform().getLocalAgentsSet()); // TODO: Add initial Agents to arguments (and eventually pass to Context?)
					agentsLists.add(planInterface.getContext(nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext.class).getOtherDFs());
					agentsLists.forEach((agents) ->
					agents.forEach((aid) -> {
						if (aid == planInterface.getAgentID()) {
							return;
						}

						nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope envelope = new nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope();
						envelope.setFrom(planInterface.getAgentID());
						envelope.addTo(aid);
						envelope.addIntendedReceiver(aid);

						ACLMessage message = new ACLMessage(Performative.PROPOSE);
						message.addReceiver(aid);
						message.addReplyTo(planInterface.getAgentID());
						message.setSender(planInterface.getAgentID());
						message.setContent("propose-registration"); // NOTE: Not used! The 'PROPOSE' performative is enough for now.
						message.setEnvelope(envelope);

						try {
							planInterface.getAgent().sendMessage(message);
						} catch (Exception ex) {
							logger.log(DirectoryFacilitator.class, ex);
						}
					}));
				} catch (Exception ex) {
					logger.log(DirectoryFacilitator.class, Level.WARNING, ex);
				}
				return null;
			}
		};
	}

	private static void replySubscriber(
		PlanToAgentInterface<Void> planInterface,
		AgentID aid,
		String conversationId,
		Performative replyPerformative,
		String serviceType,
		Collection<AgentID> services) {
		
		StringBuilder sb = new StringBuilder();
		services.forEach( (serviceId) -> sb.append( serviceId.toString() + " " ) );
		
		final nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope envelope = new nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope();
		envelope.setFrom(planInterface.getAgentID());
		envelope.addTo(aid);
		envelope.addIntendedReceiver(aid);

		final ACLMessage response = new ACLMessage(replyPerformative);
		response.setSender(planInterface.getAgentID());
		response.addReceiver(aid);
		response.setConversationId(conversationId);
		response.setContent(serviceType + " " + sb.toString());
		response.setEnvelope(envelope);

		logger.log(DirectoryFacilitator.class, Level.FINER, aid.toString() + " <<< " + replyPerformative.toString() + " |" + serviceType + " " + sb.toString());
		
		try {
			planInterface.getAgent().sendMessage(response);
		} catch (Exception ex) {
			logger.log(DirectoryFacilitator.class, Level.WARNING, ex);
		}
	}

	private static void forwardToOtherDf(
		PlanToAgentInterface<Void> planInterface,
		Collection<AgentID> otherDfs,
		String conversationId,
		String forwardedContent) {
		
		otherDfs.forEach((AgentID recipientAid) -> {
			final nl.uu.cs.iss.ga.sim2apl.core.fipa.mts.Envelope envelope = new Envelope();
			envelope.setFrom(planInterface.getAgentID());
		    envelope.addTo(recipientAid);
			envelope.addIntendedReceiver(recipientAid);
			
			final ACLMessage response = new ACLMessage(Performative.PROXY);
			response.setSender(planInterface.getAgentID());
		    response.addReceiver(recipientAid);
		    response.setConversationId(conversationId);
			response.setContent(forwardedContent);
			response.setEnvelope(envelope);
			
			try {
				planInterface.getAgent().sendMessage(response);
			} catch (Exception ex) {
				logger.log(DirectoryFacilitator.class, Level.WARNING, ex);
			}
		});
	}

	private static nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext getDFContext(PlanToAgentInterface<Void> planInterface) {
		return planInterface.getContext(nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext.class);
	}

	/** Messages to the DF mostly follow the same structure. */
	private static MessageContent parseMessageContent(nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext dfContext , String content) {
		MessageContent result = new MessageContent();
		
		try {
		
			String[] words = content.split(" ");
			int numWords = words.length;
			
			result.requestType = RequestType.valueOf(words[0].toUpperCase());
			result.agentId = new AgentID(new URI(words[1]));
			for (int i = 2; i < numWords; ++i) {
				result.serviceNames.add(words[i]);
			}
			
			//dfContext.resetNickName( result.agentId );
			
		} catch (URISyntaxException ex) {
			result.requestType = RequestType.UNKOWN;
			logger.log(DirectoryFacilitator.class, Level.WARNING, "Couldn't parse message-content: " + content);
			logger.log(DirectoryFacilitator.class, Level.WARNING, "  \\-> reason: " + ex.getReason());
		}
		
		return result;
	}
	
	private static SubPlanInterface<Void> handleMessageScheme(final Trigger trigger,
			final AgentContextInterface<Void> contextInterface) {
		if (trigger instanceof ACLMessage) {
			final ACLMessage received = (ACLMessage) trigger;
			final Performative performative = received.getPerformative();
			
			switch (performative) {

			case PROXY:
				// NOTE: Intentional fallthrough.

			case CANCEL:
				// NOTE: Intentional fallthrough.
				
			case SUBSCRIBE:
				return (planInterface) -> {
					
					logger.log(DirectoryFacilitator.class, Level.FINER, received.getEnvelope().getFrom().toString() + " ??? " + performative.toString() + " |" + received.getContent());
					
					final nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext dfContext = getDFContext(planInterface);
					//dfContext.saveNickname(received.getEnvelope().getFrom());
					//received.getEnvelope().getAllTo().forEachRemaining( (aid) -> dfContext.saveNickname(aid) );
					
					final MessageContent content = parseMessageContent(dfContext, received.getContent());
					
					boolean asProxy = (performative == Performative.PROXY);
					Performative replyPerformative = Performative.FAILURE;
					
					for (String service : content.serviceNames) {
						
						Set<AgentID> subsAgents = new HashSet<>();
						Set<AgentID> serviceAgents = new HashSet<>();
						
						switch(content.requestType) {
						
						case SUBSCRIBER_ADD:
							subsAgents.add(content.agentId);
							serviceAgents.addAll( dfContext.subscribe(service, content.agentId) );
							replyPerformative = Performative.INFORM;
							break;
							
						case SUBSCRIBER_REMOVE:
                            dfContext.unsubscribe(service, content.agentId);
                            replyPerformative = Performative.AGREE; // Not actually sent presently.
							break;
							
						case SERVICE_ADD:
							subsAgents.addAll( dfContext.register(service, content.agentId) );
							serviceAgents.add(content.agentId);
							replyPerformative = Performative.INFORM;
							break;
						
						case KILL_AGENT:
							dfContext.unsubscribe(content.agentId);
							// NOTE: Intentional fallthrough!
							
						case SERVICE_REMOVE:
							subsAgents.addAll( dfContext.deregister(content.agentId) );
							serviceAgents.add(content.agentId);
							replyPerformative = Performative.CANCEL;
							break;
							
						case UNKOWN:
							logger.log(DirectoryFacilitator.class, Level.WARNING, "Unknown SUBSCRIBE request-type.");
							break;
							
						}
						final Performative replyPerformativeFinal = replyPerformative;
						
						if (serviceAgents.size() > 0) { // NOTE: Should we sent something if 0 (with a different performative maybe)? Or just remove this 'if' and let the list be empty?
							subsAgents.forEach( (subsAgent) ->
							    replySubscriber( planInterface, subsAgent, received.getConversationId(), replyPerformativeFinal, service, serviceAgents )
							);
						}
					}
					
					if (! asProxy) { // If we're not a proxy ourselves this turn, notify other known DFs of the request.
						forwardToOtherDf( planInterface, dfContext.getOtherDFs(), received.getConversationId(), received.getContent() );
					}

					return null;
				};
			
			case PROPOSE: // Some other DF just proposed that we should register.
				          // If it's on another system, this can only be because we're already known to it, so just add the sender to 'otherDF's.
				          // (NOTE: Would this behaviour ever change? -> .. but we're probably going to redo this anyway once we start to build the federated network.) 
				return (planInterface) -> {
					planInterface.getContext(nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitatorContext.class).addOtherDF(received.getSender());
					logger.log(DirectoryFacilitator.class, Level.FINER, "DF: I now know another DF (via remote Message): " + received.getSender());
					return null;
				};
			
//			case QUERY_REF: // Answer 'search' query for a service.
//				return (planInterface) -> {
//              // .... TODO?
//				};
	
			case NOT_UNDERSTOOD:
				return (planInterface) -> {
					logger.log(DirectoryFacilitator.class, Level.WARNING, "Someone didn't understand a message I sent!");
					// TODO: Send message back.
					return null;
				};
				
			default: // No other performatives are understood.
				return (planInterface) -> {
					logger.log(DirectoryFacilitator.class, Level.WARNING, "DF: Performative " + performative + " not understood!");
//					replyMessage(planInterface, received, Performative.NOT_UNDERSTOOD,
//							"Performative not supported by DirectoryFacilitator."); // TODO what is our message-content
//																					// onthology?
					return null;
				};
			}
		} else {
			return SubPlanInterface.UNINSTANTIATED();
		}
	}
	
	private static SubPlanInterface<Void> handleExternalScheme(final Trigger trigger, final AgentContextInterface<Void> contextInterface) {
		if (trigger instanceof ReceiveRemoteAddress) {
			final AgentID remoteID = ((ReceiveRemoteAddress) trigger).getAgentID();
			return (planInterface) -> {
				planInterface.getContext(DirectoryFacilitatorContext.class).addOtherDF(remoteID);
				logger.log(DirectoryFacilitator.class, Level.FINER, "DF: I now know another DF (directly by NetNode): " + remoteID);
				return null;
			};
		} else {
			return SubPlanInterface.UNINSTANTIATED();
		}
	}
	
	public DirectoryFacilitator(Platform p, Set<AgentID> others, AgentID agentID) throws AgentCreationFailedException {
		super(p, CreateAgentArguments(others), agentID);
		p.registerDirectoryFacilitator(this);
	}

	public DirectoryFacilitator(Platform p, Set<AgentID> others) throws AgentCreationFailedException, URISyntaxException {
		super(p, CreateAgentArguments(others));
		p.registerDirectoryFacilitator(this);
	}
	
	public DirectoryFacilitator(Platform p) throws AgentCreationFailedException, URISyntaxException {
		this(p, null);
	}
}
