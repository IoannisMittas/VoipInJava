package transmitter.audio;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.LineUnavailableException;

public class AudioTransmitterTests {
	private static final long WAITING_TIME_MILLIS = 5000;

	public static void main(String[] args) {
		BlockingQueue<byte[]> recordSharedQueue = new LinkedBlockingQueue<byte[]>();
		AudioRecorder recorder = new AudioRecorder(recordSharedQueue);
		MockAudioTransmitter_SaveThenPlayAudioFromRecorder transmitter = new MockAudioTransmitter_SaveThenPlayAudioFromRecorder(
				recordSharedQueue, null);

		// Start record thread
		Thread recordThread = new Thread(recorder);
		System.out.println("Recording...");
		recordThread.start();

		// Start trasmit thread
		Thread transmitThread = new Thread(transmitter);
		System.out.println("Transmitting...");
		transmitThread.start();

		// Wait some time
		try {
			Thread.sleep(WAITING_TIME_MILLIS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Play audio saved in transmitter
		try {
			System.out.println("Playing...");
			transmitter.mockPlay();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}
