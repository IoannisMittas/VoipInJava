package transmitter.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import compression.AQDPCM_Compressor;

public class AudioTransmitter implements Runnable {
	private static final int PACKET_BUFFER_SIZE = 128;
	private boolean transmitting;
	private DatagramSocket localSocket;
	private InetAddress remoteAddress;
	private int remotePort;
	private AQDPCM_Compressor compressor;
	private final BlockingQueue<byte[]> recordSharedQueue; /*- Shared with AudioRecorder */

	public AudioTransmitter(BlockingQueue<byte[]> recordSharedQueue, DatagramSocket localSocket,
			InetAddress remoteAddress, int remotePort) {
		this.recordSharedQueue = recordSharedQueue;
		this.localSocket = localSocket;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		compressor = new AQDPCM_Compressor();
	}

	@Override
	public void run() {
		start();
	}

	/*
	 * Start consuming audio from local recorder, compressing it and
	 * transmitting it to remote client
	 */
	public void start() {
		transmitting = true;
		while (transmitting) {
			byte[] originalAudio = consumeAudioFromRecorder();

			byte[] compressedAudio = compressor.compress(originalAudio);

			// transmit audio to remote client
			DatagramPacket[] packets = constructPackets(compressedAudio);
			// ... send packets
			for (int packetIndex = 0; packetIndex < packets.length; packetIndex++) {
				try {
					localSocket.send(packets[packetIndex]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void stop() {
		transmitting = false;
	}

	public byte[] consumeAudioFromRecorder() {
		byte[] recordedAudio = null;
		try {
			recordedAudio = recordSharedQueue.take();
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();

			e.printStackTrace();
		}

		return recordedAudio;
	}

	public DatagramPacket[] constructPackets(byte[] dataBuffer) {
		int packetCount = dataBuffer.length / PACKET_BUFFER_SIZE;
		DatagramPacket[] packets = new DatagramPacket[packetCount];
		for (int packetIndex = 0; packetIndex < packetCount; packetIndex++) {
			int bottomIndex = PACKET_BUFFER_SIZE * packetIndex;
			int topIndex = bottomIndex + PACKET_BUFFER_SIZE;
			byte[] packetBuffer = Arrays.copyOfRange(dataBuffer, bottomIndex, topIndex);

			DatagramPacket currentPacket = new DatagramPacket(packetBuffer, packetBuffer.length, remoteAddress,
					remotePort);

			packets[packetIndex] = currentPacket;
		}

		return packets;
	}
}
