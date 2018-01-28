package transmitter.audio;

import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class MockAudioTransmitter_SaveThenPlayAudioFromRecorder implements Runnable {
	private boolean transmitting;
	private DatagramSocket clientSocket;
	private final BlockingQueue<byte[]> recordQueue; // Shared with
														// AudioRecorder
	private ByteArrayOutputStream mockRecordBytes = new ByteArrayOutputStream();

	public MockAudioTransmitter_SaveThenPlayAudioFromRecorder(BlockingQueue<byte[]> recordQueue,
			DatagramSocket clientSocket) {
		this.recordQueue = recordQueue;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		start();
	}

	/*
	 * Start consuming audio from local recorder, encoding it and transmitting
	 * it to remote client
	 */
	public void start() {
		transmitting = true;
		while (transmitting) {
			byte[] beforeEncodingAudio = consumeAudioFromRecorder();

			// Encode audio

			mockSaveAudio(beforeEncodingAudio);

			// Transmit audio to remote client

		}
	}

	public void stop() {
		transmitting = false;
	}

	public byte[] consumeAudioFromRecorder() {
		byte[] recordedAudio = null;
		try {
			recordedAudio = recordQueue.take();
		} catch (InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();

			e.printStackTrace();
		}

		return recordedAudio;
	}

	public AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		int sampleSizeInBits = 16;
		int channels = 2;
		boolean signed = true;
		boolean bigEndian = true;

		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	public void mockSaveAudio(byte[] recordBuffer) {
		mockRecordBytes.write(recordBuffer, 0, recordBuffer.length);
	}

	public void mockPlay() throws LineUnavailableException {
		AudioFormat audioFormat = getAudioFormat();

		// Check if the system supports the data line
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(lineInfo)) {
			throw new LineUnavailableException("The system does not support the specified audio format.");
		}

		SourceDataLine audioOutputLine = AudioSystem.getSourceDataLine(audioFormat);
		audioOutputLine.open(audioFormat);
		audioOutputLine.start();

		// Play audio
		byte[] record = mockRecordBytes.toByteArray();
		audioOutputLine.write(record, 0, record.length);

		audioOutputLine.drain();
		audioOutputLine.stop();
		audioOutputLine.close();
	}

	// encode ------------------------------------------

}
