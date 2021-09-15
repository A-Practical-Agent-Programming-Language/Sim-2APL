package nl.uu.cs.iss.ga.sim2apl.core.platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import nl.uu.cs.iss.ga.sim2apl.core.defaults.messenger.MessageReceiverNotFoundException;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.MessageInterface;
import nl.uu.cs.iss.ga.sim2apl.core.messaging.Messenger;
import nl.uu.cs.iss.ga.sim2apl.core.agent.Agent;
import nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitator;
import nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.ReceiveRemoteAddress;
import nl.uu.cs.iss.ga.sim2apl.core.logging.Loggable;

/**
 * Prototype/Ad-Hoc/Hack solution for multiple platforms, in preparation for a
 * robust system.
 */
public class NetNode<X, T extends MessageInterface> implements Messenger<T, X>, Runnable {

	protected final Loggable logger = Platform.getLogger();
	protected final static ExecutorService THREAD_SERVICE = Executors.newCachedThreadPool();

	protected String host;
	protected int port;
	protected Messenger<T, X> innerMessenger;
	protected boolean listening = true;

	protected nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitator yellowPages;
	
	public NetNode(Messenger<T, X> localMessenger, String host, int port) {
		this.host = host;
		this.port = port;
		this.innerMessenger = localMessenger;

		if (!innerMessenger.implementsEncoding()) {
			throw new IllegalArgumentException("Messenger must support encoding/decoding.");
		}

		yellowPages = null;
		
		THREAD_SERVICE.submit(this);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			
			logger.log(getClass(), Level.INFO, "Started server on port " + port + " | " + serverSocket.getInetAddress().getHostAddress());

			while (listening) {
				
				try(Socket servSocket = serverSocket.accept()) {
					
					try (DataInputStream dis = new DataInputStream(servSocket.getInputStream())){						
						int numBytes = dis.readInt();
						switch(numBytes) {
						case -1: // Got a request (from another Platform) for DF ('Yellow Pages') ID, with the remote Yellow Pages ID embedded into the request.
							if (yellowPages == null) {
								// TODO: Give back some error(code) or start waiting until there _is_ a DF.
								break;
							}
							
							String remoteHost = dis.readUTF();
							int remotePort = dis.readInt();
							
							try (Socket clientSocket = new Socket(remoteHost, remotePort)) { //InetAddress.getByAddress(remoteHost), remotePort)) {
								try(DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
									dos.writeInt(-2); // See 'case -2' below.
									dos.writeUTF(yellowPages.getAID().toString());
								}

							} catch (UnknownHostException ex) {
								logger.log(getClass(), Level.WARNING, "NET2APL NetNode can't read remote Yellow Pages address error: " + ex.getMessage());
							} catch (IOException ex) {
								logger.log(getClass(), Level.WARNING, "NET2APL NetNode can't send to remote Yellow Pages error: " + ex.getMessage());
							}
							//NOTE: Intentional fallthrough!
							
						case -2: // Got a reply with the ID of a DF on another Platform. 
							try {
								nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID remoteYellowPages = new nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID(dis.readUTF());
								yellowPages.addExternalTrigger(new ReceiveRemoteAddress(remoteYellowPages));
							} catch (URISyntaxException ex) {
								logger.log(getClass(), Level.WARNING, "NET2APL NetNode can't read remote Yellow Pages AgentID error: " + ex.getMessage());
							}							
							break;
							
						default: // Got a 'normal' message.
							byte[] data = new byte[numBytes];
							dis.readNBytes(data, 0, numBytes);
													
							this.deliverMessage(this.decodeMessage(data));
							break;
						}
					}
				}
			}
			
			logger.log(getClass(), Level.INFO, "NET2APL Netnode server stopped");
			
		} catch (IOException ex) {
			// TODO: logging/ error-handling
			logger.log(getClass(), Level.SEVERE, "NET2APL NetNode server error: " + ex.getMessage());
		} catch (MessageReceiverNotFoundException ex) {
			// TODO: logging/ error-handling
			logger.log(getClass(), Level.SEVERE, "NET2APL NetNode internal error: " + ex.getMessage());
		}
	}
	
	public void stop() {
		this.listening = false;
	}

	public void requestRemoteID(String remoteHost, int remotePort) {
		if (yellowPages == null) {
			// TODO: Give back some error(code) or start waiting until there _is_ a DF.
		}
		
		try (Socket clientSocket = new Socket(remoteHost, remotePort)) { //InetAddress.getByAddress(remoteHost), remotePort)) {
			try(DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
				dos.writeInt(-1);
				dos.writeUTF(host);
				dos.writeInt(port);
				dos.writeUTF(yellowPages.getAID().toString());
			}

		} catch (UnknownHostException ex) {
			logger.log(getClass(), Level.WARNING, "NET2APL NetNode can't read remote Yellow Pages address error: " + ex.getMessage());
		} catch (IOException ex) {
			logger.log(getClass(), Level.WARNING, "NET2APL NetNode can't send to remote Yellow Pages error: " + ex.getMessage());
		}
	}
	
	@Override
	public void agentDied(nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID agentID) {
		innerMessenger.agentDied(agentID);
	}

	@Override
	public void register(Agent<X> agent) {
		if (agent instanceof nl.uu.cs.iss.ga.sim2apl.core.fipa.ams.DirectoryFacilitator) {
			yellowPages = (DirectoryFacilitator) agent;
		}
		innerMessenger.register(agent);
	}

	@Override
	public void deregister(nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID agentID) {
		innerMessenger.deregister(agentID);
	}

	@Override
	public void deliverMessage(nl.uu.cs.iss.ga.sim2apl.core.agent.AgentID receiver, T message) throws MessageReceiverNotFoundException {
		String remoteHost = receiver.getHost();
		int remotePort = receiver.getPort();

		if (host.equals(remoteHost) && port == remotePort) {
			innerMessenger.deliverMessage(receiver, message);
		} else {
			try (Socket clientSocket = new Socket(remoteHost, remotePort)) {
				byte[] asbytes = this.encodeMessage(message);
								
				try(DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
					dos.writeInt(asbytes.length);
					dos.write(asbytes);
				}

			} catch (UnknownHostException ex) {
				logger.log(getClass(), Level.SEVERE, "NET2APL NetNode send error: " + ex.getMessage());
			} catch (IOException ex) {
				logger.log(getClass(), Level.SEVERE, "NET2APL NetNode server error: " + ex.getMessage());
			}
		}
	}

	@Override
	public void deliverMessage(T message) throws MessageReceiverNotFoundException {
		for (AgentID receiver : message.getReceiver()) {
			this.deliverMessage(receiver, message);
		}
	}

	@Override
	public boolean implementsEncoding() {
		return innerMessenger.implementsEncoding(); // NOTE: Should be always true as of writing.
	}

	@Override
	public byte[] encodeMessage(T message) throws UnsupportedOperationException {
		return innerMessenger.encodeMessage(message);
	}

	@Override
	public T decodeMessage(byte[] asBytes) throws UnsupportedOperationException {
		return innerMessenger.decodeMessage(asBytes);
	}

}
