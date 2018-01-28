package connection.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client {
	private static final String SERVER_ADDRESS = "INSERT HERE" // TODO
	private static final int PACKET_BUFFER_SIZE = 4096;
	private boolean isConnected;
	private InetAddress serverAddress;
	private int serverPort;
	private InetAddress relayAddress;
	private int relayPort;
	private InetAddress peerAddress;
	private int peerPort;
	private DatagramSocket socket;

	public Client() {
		// create random socket
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		try {
			 //serverAddress = InetAddress.getLocalHost();
			serverAddress = InetAddress.getByName(SERVER_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		serverPort = 45000;
	}

	/*
	 * Request communication with remote peer(s) from server.
	 */
	public void requestCommunication() {
		int serverResponseTimeout = 0;
		try {
			socket.setSoTimeout(serverResponseTimeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		isConnected = false;

		// send request packet to server
		byte[] requestInfo = new byte[PACKET_BUFFER_SIZE];
		DatagramPacket requestPacket = new DatagramPacket(requestInfo, requestInfo.length, serverAddress, serverPort);
		try {
			socket.send(requestPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// wait for server response packet
		byte[] response = new byte[PACKET_BUFFER_SIZE];
		DatagramPacket serverResponsePacket = new DatagramPacket(response, response.length);
		try {
			socket.receive(serverResponsePacket);
			setRelayAddressAndPort(serverResponsePacket);
			isConnected = true;
		} catch (SocketTimeoutException e) {
			// didn't receive server response, do nothing
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Used in case of TURN relay server
	 */
	private void setRelayAddressAndPort(DatagramPacket serverResponsePacket) {
		this.relayAddress = serverResponsePacket.getAddress();

		byte[] response = serverResponsePacket.getData();
		int actualResponseLength = serverResponsePacket.getLength();
		byte[] relayPortAsBytes = Arrays.copyOf(response, actualResponseLength);
		String relayPortAsString = new String(relayPortAsBytes);
		this.relayPort = Integer.parseInt(relayPortAsString);
	}

	/*
	 * Used in case of UDP hole punch (direct communication between client and
	 * peers, not using relay server)
	 */
	private void setPeerAddressAndPort(DatagramPacket serverResponsePacket) {
		byte[] response = serverResponsePacket.getData();
		int actualResponseLength = serverResponsePacket.getLength();
		byte[] peerAddressAndPortAsBytes = Arrays.copyOf(response, actualResponseLength);

		String peerAddressAndPortAsString = new String(peerAddressAndPortAsBytes);

		String[] splitPeerAddressAndPort = peerAddressAndPortAsString.split("-");

		try {
			this.peerAddress = InetAddress.getByName(splitPeerAddressAndPort[0]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		this.peerPort = Integer.parseInt(splitPeerAddressAndPort[1]);
	}

	public boolean isConnected() {
		return isConnected;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public InetAddress getRelayAddress() {
		return relayAddress;
	}

	public int getRelayPort() {
		return relayPort;
	}

	public InetAddress getPeerAddress() {
		return peerAddress;
	}

	public int getPeerPort() {
		return peerPort;
	}
}
