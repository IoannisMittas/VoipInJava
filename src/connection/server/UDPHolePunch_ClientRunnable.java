package connection.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

public class UDPHolePunch_ClientRunnable implements Runnable {
	private InetAddress clientAddress;
	private int clientPort;
	private DatagramSocket serverSocket;
	private List<Map.Entry<InetAddress, Integer>> onlineClients; /*- structured as: IP address, port */

	public UDPHolePunch_ClientRunnable(DatagramPacket clientRequestPacket, List<Map.Entry<InetAddress, Integer>> onlineClients,
			DatagramSocket serverSocket) {
		this.clientAddress = clientRequestPacket.getAddress();
		this.clientPort = clientRequestPacket.getPort();
		this.onlineClients = onlineClients;
		this.serverSocket = serverSocket;
	}

	@Override
	public void run() {
		sendPeerInfoToClient();
	}

	/*
	 * Inform client about the IP address and port of the remote peer(s) he
	 * wants to connect with. The method implements the UDP hole punching
	 * technique.
	 */
	private void sendPeerInfoToClient() {
		// construct info package
		Map.Entry<InetAddress, Integer> entry = randomPeer();
		InetAddress peerAddress = entry.getKey();
		Integer peerPort = entry.getValue();
		String peerAddressAndPort = peerAddress.getHostAddress() + "-" + String.valueOf(peerPort);
		byte[] buffer = peerAddressAndPort.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
		
		// send package to client
		try {
			serverSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Find a random peer to connect and remove him from the list of online clients.
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
