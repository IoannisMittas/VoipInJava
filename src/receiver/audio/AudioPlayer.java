package receiver.audio;

import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer implements Runnable {
	private boolean playing;
	private AudioFormat audioFormat;
	private SourceDataLine audioOutputLine;
	private final BlockingQueue<byte[]> receivedAudioSharedQueue; /*- Shared with AudioReceiver */

	public AudioPlayer(BlockingQueue<byte[]> receivedAudioSharedQueue) {
		this.receivedAudioSharedQueue = receivedAudioSharedQueue;
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
	 * Start playing audio passed from receiver.
	 * 
	 * @throws LineUnavailableException if the system does not support the
	 * specified audio format or can't open the audio data line.
	 * 
	 */
	public void start() throws LineUnavailableException {
		audioFormat = getAudioFormat();

		// Check if the system supports the data line
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(lineInfo)) {
			throw new LineUnavailableException("The system does not support the specified audio format.");
		}

		audioOutputLine = AudioSystem.getSourceDataLine(audioFormat);
		audioOutputLine.open(audioFormat);
		audioOutputLine.start();

		playing = true;
		while (playing) {
			byte[] buffer = consumeAudioFromReceiver();
			
			// play audio
			audioOutputLine.write(buffer, 0, buffer.length);
		}

	}

	public void stop() {
		playing = false;
		
		audioOutputLine.drain();
		audioOutputLine.stop();
		audioOutputLine.close();
	}

	public byte[] consumeAudioFromReceiver() {
		byte[] buffer = null;
		try {
			buffer = receivedAudioSharedQueue.take();
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();
			
			e.printStackTrace();
		}
		
		return buffer;
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
