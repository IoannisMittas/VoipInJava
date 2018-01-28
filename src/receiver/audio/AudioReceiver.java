package receiver.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import compression.AQDPCM_Decompressor;

public class AudioReceiver implements Runnable {
	private static final int PACKET_BUFFER_SIZE = 128;
	private boolean receiving;
	private DatagramSocket localSocket;
	private AQDPCM_Decompressor decompressor;
	private final BlockingQueue<byte[]> receivedAudioSharedQueue; /*- Shared with AudioPlayer */

	public AudioReceiver(BlockingQueue<byte[]> receivedAudioSharedQueue, DatagramSocket localSocket) {
		this.receivedAudioSharedQueue = receivedAudioSharedQueue;
		this.localSocket = localSocket;
		decompressor = new AQDPCM_Decompressor();
	}

	@Override
	public void run() {
		start();
	}

	/*
	 * Start receiving audio from remote client, decompressing it and passing it
	 * to local player
	 */
	public void start() {
		receiving = true;
		while (receiving) {
			// receive audio from remote client
			byte[] receivedAudio = new byte[PACKET_BUFFER_SIZE];
			DatagramPacket packet = new DatagramPacket(receivedAudio, receivedAudio.length);
			try {
				localSocket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			int actualReceivedLength = packet.getLength();
			byte[] compressedAudio = Arrays.copyOf(receivedAudio, actualReceivedLength);
			
			byte[] originalAudio = decompressor.decompress(compressedAudio);

			produceAudioForPlayer(originalAudio);
		}
	}

	public void stop() {
		receiving = false;
	}

	public void produceAudioForPlayer(byte[] originalAudio) {
		try {
			receivedAudioSharedQueue.put(originalAudio);
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();

			e.printStackTrace();
		}
	}

}
