package transmitter.audio;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class AudioRecorder implements Runnable {
	private static final int BUFFER_SIZE = 4096;
	private boolean recording;
	private AudioFormat audioFormat;
	private TargetDataLine audioInputLine;
	private final BlockingQueue<byte[]> recordSharedQueue; /*- Shared with AudioTransmitter */

	public AudioRecorder(BlockingQueue<byte[]> recordSharedQueue) {
		this.recordSharedQueue = recordSharedQueue;
	}

	@Override
	public void run() {
		try {
			start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();

			System.exit(-1);
		}
	}

	/*
	 * Start recording audio and passing it to transmitter.
	 * 
	 * @throws LineUnavailableException if the system does not support the
	 * specified audio format or can't open the audio data line.
	 */
	public void start() throws LineUnavailableException {
		audioFormat = getAudioFormat();
		
		// Check if system supports the data line
		DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(lineInfo)) {
			throw new LineUnavailableException("The system does not support the specified audio format.");
		}
		
		audioInputLine = AudioSystem.getTargetDataLine(audioFormat);
		audioInputLine.open(audioFormat);
		audioInputLine.start();

		recording = true;
		while (recording) {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = audioInputLine.read(buffer, 0, buffer.length);
			produceAudioForTransmitter(buffer, bytesRead);
		}
	}

	public void stop() {
		recording = false;
		
		audioInputLine.stop();
		audioInputLine.close();
	}

	public void produceAudioForTransmitter(byte[] buffer, int bytesRead) {
		// Truncate record buffer to contain bytes actually read
		byte[] truncatedBuffer = Arrays.copyOf(buffer, bytesRead);
		
		try {
			recordSharedQueue.put(truncatedBuffer);
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();
			
			e.printStackTrace();
		}
	}

	public AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		int sampleSizeInBits = 16;
		int channels = 2;
		boolean signed = true;
		boolean bigEndian = true;

		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}
}
