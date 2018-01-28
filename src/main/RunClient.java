package main;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import connection.client.Client;
import gui.client.MainFrame;
import receiver.audio.AudioPlayer;
import receiver.audio.AudioReceiver;
import transmitter.audio.AudioRecorder;
import transmitter.audio.AudioTransmitter;

public class RunClient {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new MainFrame();
			}
		});
		
		Client client = new Client();
		client.requestCommunication();
		
		if (!client.isConnected()) {
			System.out.println("Couldn't establish connection. The program will exit.");
			System.exit(-1);
		}
		
		System.out.println("Connected with peer");
		DatagramSocket clientSocket = client.getSocket();
		InetAddress relayAddress = client.getRelayAddress();
		int relayPort = client.getRelayPort();

		BlockingQueue<byte[]> recordSharedQueue = new LinkedBlockingQueue<byte[]>();
		
		// start record thread
		AudioRecorder recorder = new AudioRecorder(recordSharedQueue);
		Thread recordThread = new Thread(recorder);
		recordThread.start();
		System.out.println("Recording...");
		
		// start transmit thread
		AudioTransmitter transmitter = new AudioTransmitter(recordSharedQueue, clientSocket, relayAddress, relayPort);
		Thread transmitThread = new Thread(transmitter);
		transmitThread.start();
		System.out.println("Transmitting...");

		BlockingQueue<byte[]> receivedAudioSharedQueue = new LinkedBlockingQueue<byte[]>();
		
		// start receive thread
		AudioReceiver receiver = new AudioReceiver(receivedAudioSharedQueue, clientSocket);
		Thread receiveThread = new Thread(receiver);
		receiveThread.start();
		System.out.println("Receiving...");
		
		// start play thread
		AudioPlayer player = new AudioPlayer(receivedAudioSharedQueue);
		Thread playThread = new Thread(player);
		playThread.start();
		System.out.println("Playing...");
	}
}
