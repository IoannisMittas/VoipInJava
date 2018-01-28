package compression;

public class AQDPCM_Decompressor {
	private int previousIndex; // Index into step size table
	private short previousSample; // Predicted sample
	private byte[] indexTable; // Table of index changes
	private int[] stepSizeTable; // Quantizer step size lookup table

	public AQDPCM_Decompressor() {
		previousIndex = 0;
		previousSample = 0;

		indexTable = new byte[] { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8 };

		stepSizeTable = new int[] { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55,
				60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449,
				494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499,
				2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635,
				13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };
	}

	public byte[] decompress(byte[] compressedData) {
		int originalSamplesLength = compressedData.length * 2;
		short[] originalSamples = new short[originalSamplesLength];

		int originalSamplesIndex = 0;
		for (int compressedDataIndex = 0; compressedDataIndex < compressedData.length; compressedDataIndex++) {
			byte twoCompressedNibbles = compressedData[compressedDataIndex];

			byte upperNibble = (byte) ((twoCompressedNibbles >>> 4) & 0x0F);
			short firstSample = (short) legacyADPCMDecoder(upperNibble);
			originalSamples[originalSamplesIndex] = firstSample;
			originalSamplesIndex++;

			byte lowerNibble = (byte) (twoCompressedNibbles & 0x0F);
			short secondSample = (short) legacyADPCMDecoder(lowerNibble);
			originalSamples[originalSamplesIndex] = secondSample;
			originalSamplesIndex++;
		}

		byte[] originalData = convertShortsToBigEndianBytes(originalSamples);

		return originalData;
	}

	private byte[] convertShortsToBigEndianBytes(short[] samples) {
		int bigEndianBytesLength = samples.length * 2;
		byte[] bigEndianBytes = new byte[bigEndianBytesLength];

		int bytesIndex = 0;
		for (int samplesIndex = 0; samplesIndex < samples.length; samplesIndex++) {
			short sample = samples[samplesIndex];

			byte mostSignificant = (byte) ((sample >>> 8) & 0x00FF);
			bigEndianBytes[bytesIndex] = mostSignificant;
			bytesIndex++;

			byte leastSignificant = (byte) (sample & 0x00FF);
			bigEndianBytes[bytesIndex] = leastSignificant;
			bytesIndex++;
		}

		return bigEndianBytes;
	}

	private int legacyADPCMDecoder(byte code) {
		int step; // quantizer step size
		int predictedSample; /* Output of ADPCM predictor */
		int diffQ; /* Dequantized predicted difference */
		int index; /* Index into step size table */

		/*
		 * Restore previous values of predicted sample and quantizer step size
		 * index
		 */
		predictedSample = (int) (previousSample);
		index = previousIndex;

		/*
		 * Find quantizer step size from lookup table using index
		 */
		step = stepSizeTable[index];

		/*
		 * Inverse quantize the ADPCM code into a difference using the quantizer
		 * step size
		 */
		diffQ = step >> 3;
		if ((code & 4) != 0) {
			diffQ += step;
		}
		if ((code & 2) != 0) {
			diffQ += step >> 1;
		}
		if ((code & 1) != 0) {
			diffQ += step >> 2;
		}

		/*
		 * Add the difference to the predicted sample
		 */
		if ((code & 8) != 0) {
			predictedSample -= diffQ;
		} else {
			predictedSample += diffQ;
		}

		/*
		 * Check for overflow of the new predicted sample
		 */
		if (predictedSample > 32767) {
			predictedSample = 32767;
		} else if (predictedSample < -32767) {
			predictedSample = -32767;
		}

		/*
		 * Find new quantizer step size by adding the old index and a table
		 * lookup using the ADPCM code
		 */
		index += indexTable[code];

		/*
		 * Check for overflow of the new quantizer step size index
		 */
		if (index < 0) {
			index = 0;
		}
		if (index > 88) {
			index = 88;
		}

		/*
		 * Save predicted sample and quantizer step size index for next
		 * iteration
		 */
		previousSample = (short) predictedSample;
		previousIndex = index;

		/* Return the new speech sample */
		return predictedSample;
	}
}
