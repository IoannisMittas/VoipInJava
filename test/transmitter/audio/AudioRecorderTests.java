package transmitter.audio;

import javax.sound.sampled.LineUnavailableException;

public class AudioRecorderTests {
	private static final long RECORDING_TIME_MILLIS = 5000;

	public static void main(String[] args) {
		// Create a separate record thread
		final MockAudioRecorder_SaveThenPlayAudioLocally recorder = new MockAudioRecorder_SaveThenPlayAudioLocally(
				null);
		Thread recordThread = new Thread(recorder);

		recordThread.start();

		// Record for specific time
		try {
			Thread.sleep(RECORDING_TIME_MILLIS);
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();

			e.printStackTrace();
		}

		recorder.stop();
		System.out.println("STOPPED");

		// Play saved audio
		try {
			recorder.mockPlay();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("THE END");
	}
}
