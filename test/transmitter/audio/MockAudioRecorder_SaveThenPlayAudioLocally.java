package transmitter.audio;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import compression.AQDPCM_Compressor;
import compression.AQDPCM_Decompressor;

public class MockAudioRecorder_SaveThenPlayAudioLocally implements Runnable {

	private boolean recording;
	private static final int RECORD_BUFFER_SIZE = 4096;
	private AudioFormat audioFormat;
	private TargetDataLine audioInputLine;
	private final BlockingQueue recordQueue;
	private ByteArrayOutputStream mockRecordBytes = new ByteArrayOutputStream();
	private AQDPCM_Compressor compressor;
	private AQDPCM_Decompressor decompressor;
	
	
	public MockAudioRecorder_SaveThenPlayAudioLocally(BlockingQueue recordQueue) {
		this.recordQueue = recordQueue;
	}

	@Override
	public void run() {
		try {
			System.out.println("Start recording...");
			start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * Start recording audio and pass it to transmitter.
	 * 
	 * @throws LineUnavailableException if the system does not support the
	 * specified audio format or can't open the audio data line.
	 */
	public void start() throws LineUnavailableException {
		audioFormat = getAudioFormat();

		// Check if the system supports the data line
		DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(lineInfo)) {
			throw new LineUnavailableException("The system does not support the specified audio format.");
		}

		audioInputLine = AudioSystem.getTargetDataLine(audioFormat);
		audioInputLine.open(audioFormat);
		audioInputLine.start();

		byte[] recordBuffer = new byte[RECORD_BUFFER_SIZE];
		recording = true;
		while (recording) {
			int bytesRead = audioInputLine.read(recordBuffer, 0, recordBuffer.length);

			// produceAudioForTransmitter(recordBuffer, bytesRead);

			mockSaveAudio(recordBuffer, bytesRead);
		}
	}

	public void stop() {
		recording = false;

		audioInputLine.stop();
		audioInputLine.close();
	}

	public void produceAudioForTransmitter(byte[] recordBuffer, int bytesRead) {
		// Truncate record buffer to contain bytes actually read
		byte[] truncatedBuffer = Arrays.copyOf(recordBuffer, bytesRead);

		try {
			recordQueue.put(truncatedBuffer);
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

	public void mockSaveAudio(byte[] recordBuffer, int bytesRead) {
		mockRecordBytes.write(recordBuffer, 0, bytesRead);
	}

	public void mockPlay() throws LineUnavailableException {
		audioFormat = getAudioFormat();

		// Check if the system supports the data line
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(lineInfo)) {
			throw new LineUnavailableException("The system does not support the specified audio format.");
		}

		SourceDataLine audioOutputLine = AudioSystem.getSourceDataLine(audioFormat);
		audioOutputLine.open(audioFormat);
		audioOutputLine.start();

		byte[] record = mockRecordBytes.toByteArray();
		System.out.println("record length: " + record.length);
		
		compressor = new AQDPCM_Compressor();
		byte[] compressedBytes = compressor.compress(record);
		System.out.println("Compression length: " + compressedBytes.length);
		
		
		decompressor = new AQDPCM_Decompressor();
		byte[] decompressedBytes = decompressor.decompress(compressedBytes);
		System.out.println("Decompression length: " + decompressedBytes.length);
		
		// Play audio
		//audioOutputLine.write(record, 0, record.length);
		audioOutputLine.write(decompressedBytes, 0, record.length);

		audioOutputLine.drain();
		audioOutputLine.stop();
		audioOutputLine.close();
	}

}
