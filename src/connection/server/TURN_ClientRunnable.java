package connection.server;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/*
 * Class implementing TURN(Traversal Using Relays around NAT) protocol. The server is responsible for dispatching 
 * packets between peers.
 */
public class TURN_ClientRunnable implements Runnable {
	private static final int PACKET_BUFFER_SIZE = 4096;
	private InetAddress clientAddress;
	private int clientPort;
	private DatagramSocket serverSocket;
	private List<Map.Entry<InetAddress, Integer>> onlineClients; /*- structured as: IP address, port */

	public TURN_ClientRunnable(DatagramPacket clientRequestPacket, List<Map.Entry<InetAddress, Integer>> onlineClients,
			DatagramSocket serverSocket) {
		this.clientAddress = clientRequestPacket.getAddress();
		this.clientPort = clientRequestPacket.getPort();
		this.onlineClients = onlineClients;
		this.serverSocket = serverSocket;
	}

	@Override
	public void run() {
		dispatchPackets();
	}

	/*
	 * Receive packets from client and send them to peer(s).
	 */
	private void dispatchPackets() {
		// wait for a random peer
		Map.Entry<InetAddress, Integer> entry = randomPeer();
		InetAddress peerAddress = entry.getKey();
		Integer peerPort = entry.getValue();

		// construct relaySocket, using the first available port-forwarded port
		DatagramSocket relaySocket = null;
		int relayPort = serverSocket.getLocalPort() + 1;
		boolean isBound = false;
		while (!isBound) {
			try {
				relaySocket = new DatagramSocket(relayPort);
			} catch (BindException e) {
				// ... couldn't bound, try the next port
				relayPort += 1;
				continue;
			} catch (SocketException e) {
				e.printStackTrace();
			}
			isBound = true;
		}

		// construct and send acceptance package to client, containing the relay
		// port
		relayPort = relaySocket.getLocalPort();
		String relayPortAsString = String.valueOf(relayPort);
		byte[] acceptanceBuffer = relayPortAsString.getBytes();
		DatagramPacket acceptancePacket = new DatagramPacket(acceptanceBuffer, acceptanceBuffer.length, clientAddress,
				clientPort);
		try {
			serverSocket.send(acceptancePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (true) {
			// receive packet from client using RELAY socket
			byte[] receiveBuffer = new byte[PACKET_BUFFER_SIZE];
			DatagramPacket clientPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			try {
				relaySocket.receive(clientPacket);

				// DEBUG
				System.out.println("Must receive from client: " + this.clientAddress + " " + this.clientPort);
				System.out.println("Received from client: " + clientPacket.getAddress() + " " + clientPacket.getPort());

			} catch (IOException e) {
				e.printStackTrace();
			}
			int actualReceivedLength = clientPacket.getLength();
			byte[] clientData = Arrays.copyOf(receiveBuffer, actualReceivedLength);

			// send packet to peer using SERVER socket
			DatagramPacket packetForPeer = new DatagramPacket(clientData, clientData.length, peerAddress, peerPort);
			try {
				serverSocket.send(packetForPeer);

				// DEBUG
				System.out.println("Sent to peer: " + peerAddress.getHostAddress() + " " + peerPort);
				System.out.println();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Find a random peer to connect and remove him from the list of online
	 * clients.
	 */
	private Map.Entry<InetAddress, Integer> randomPeer() {
		Map.Entry<InetAddress, Integer> randomPeer = null;
		boolean isFound = false;
		while (!isFound) {
			for (Map.Entry<InetAddress, Integer> entry : onlineClients) {
				InetAddress address = entry.getKey();
				Integer port = entry.getValue();

				if ((address.equals(clientAddress)) && (port.equals(clientPort))) {
					continue;
				}

				randomPeer = entry;
				isFound = true;
				break;
			}
		}
		onlineClients.remove(randomPeer);
		return randomPeer;
	}

}
