package connection.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
	private static final int PACKET_BUFFER_SIZE = 4096;
	private boolean isRunning;
	private InetAddress serverAddress;
	private int serverPort;
	private DatagramSocket serverSocket;
	private List<Map.Entry<InetAddress, Integer>> onlineClients; /*- structured as: IP address, port */

	public Server() {
		try {
			serverAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		serverPort = 45000;

		try {
			serverSocket = new DatagramSocket(serverPort, serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		onlineClients = new CopyOnWriteArrayList<>();
	}

	/*
	 * Server is waiting for a client request. When it gets one, it updates the
	 * online client list with the client and creates a new client thread.
	 */
	public void start() {
		isRunning = true;
		while (isRunning) {
			// wait for client request
			byte[] requestInfo = new byte[PACKET_BUFFER_SIZE];
			DatagramPacket clientRequestPacket = new DatagramPacket(requestInfo, requestInfo.length);
			try {
				serverSocket.receive(clientRequestPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}

			addOnlineClient(clientRequestPacket);

			// create client thread
			TURN_ClientRunnable client = new TURN_ClientRunnable(clientRequestPacket, onlineClients, serverSocket);
			new Thread(client).start();
		}
	}

	public void stop() {
		isRunning = false;
	}

	public void addOnlineClient(DatagramPacket clientRequestPacket) {
		InetAddress clientAddress = clientRequestPacket.getAddress();
		int clientPort = clientRequestPacket.getPort();
			Map.Entry<InetAddress, Integer> entry = new AbstractMap.SimpleEntry<>(clientAddress, clientPort);
			onlineClients.add(entry);
			
			// DEBUG--------------------------------------------------------------------------------
			System.out.println("Added: " + clientAddress + " " + clientPort);
	}

	public void removeOnlineClient() {
	}

}
