package compression;

public class AQDPCM_Compressor {
	private int previousIndex; // Index into step size table
	private short previousSample; // Predicted sample
	private byte[] indexTable; // Table of index changes
	private int[] stepSizeTable; // Quantizer step size lookup table

	public AQDPCM_Compressor() {
		previousIndex = 0;
		previousSample = 0;

		indexTable = new byte[] { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};

		stepSizeTable = new int[] { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55,
				60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449,
				494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499,
				2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635,
				13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };
	}

	public byte[] compress(byte[] originalData) {
		short[] samples = convertBigEndianBytesToShorts(originalData);

		int compressedDataLength = samples.length / 2;
		byte[] compressedData = new byte[compressedDataLength];

		int compressedDataIndex = 0;
		for (int sampleIndex = 0; sampleIndex < samples.length; sampleIndex += 2) {
			short firstSample = samples[sampleIndex];
			byte twoCompressedSamples = legacyADPCMEncoder(firstSample);
			// move to upper nibble
			twoCompressedSamples = (byte) ((twoCompressedSamples << 4) & 0xF0);

			short secondSample = samples[sampleIndex + 1];
			// compress the new sample and save it to lower nibble
			twoCompressedSamples |= legacyADPCMEncoder(secondSample);

			compressedData[compressedDataIndex] = twoCompressedSamples;
			compressedDataIndex++;
		}

		return compressedData;
	}

	/*
	 * Convert each original sample, which is saved as 2 bytes in big-endian,
	 * into one short
	 * 
	 * @param bigEndianBytes the array of original samples
	 * 
	 * @return an array of samples as shorts
	 */
	private short[] convertBigEndianBytesToShorts(byte[] bigEndianBytes) {
		int samplesCount = bigEndianBytes.length / 2;
		short[] samples = new short[samplesCount];

		int sampleIndex = 0;
		for (int byteIndex = 0; byteIndex < bigEndianBytes.length; byteIndex += 2) {
			short mostSignificant = bigEndianBytes[byteIndex];
			mostSignificant = (short)((mostSignificant << 8) & 0xFF00);

			short leastSignificant = bigEndianBytes[byteIndex + 1];
			leastSignificant = (short) (leastSignificant & 0x00FF);

			short currentSample = (short) (mostSignificant | leastSignificant);
			samples[sampleIndex] = currentSample;
			sampleIndex++;
		}

		return samples;
	}

	private byte legacyADPCMEncoder(short sample) {
		int code; // ADPCM output value
		int difference; // difference between sample and the predicted sample

		int step; // quantizer step size
		int predictedSample; // output of ADPCM predictor
		int differenceQ; // dequantized predicted difference
		int index; // index into step size table

		// restore previous values of predicted sample and quantizer step size
		// index
		predictedSample = (int) previousSample;
		index = previousIndex;
		step = stepSizeTable[index];

		// compute the difference between the acutal sample (sample) and the the
		// predicted sample (predictedSample)
		difference = sample - predictedSample;
		if (difference >= 0) {
			code = 0;
		} else {
			code = 8;
			difference = -difference;
		}

		// quantize the difference into the 4-bit ADPCM code using the the
		// quantizer step size
		//
		//
		// inverse quantize the ADPCM code into a predicted difference using the
		// quantizer step size
		differenceQ = step >> 3;
		if (difference >= step) {
			code |= 4;
			difference -= step;
			differenceQ += step;
		}
		step >>= 1;
		if (difference >= step) {
			code |= 2;
			difference -= step;
			differenceQ += step;
		}
		step >>= 1;
		if (difference >= step) {
			code |= 1;
			differenceQ += step;
		}

		// fixed predictor computes new predicted sample by adding the old
		// predicted sample to predicted difference
		if ((code & 8) != 0) {
			predictedSample -= differenceQ;
		} else {
			predictedSample += differenceQ;
		}

		// check for overflow of the new predicted sample
		if (predictedSample > 32767) {
			predictedSample = 32767;
		} else if (predictedSample < -32767) {
			predictedSample = -32767;
		}

		// find new quantizer stepsize index by adding the old index to a table
		// lookup using the ADPCM code
		index += indexTable[code];

		// check for overflow of the new quantizer step size index
		if (index < 0) {
			index = 0;
		}
		if (index > 88) {
			index = 88;
		}

		// save the predicted sample and quantizer step size index for next
		// iteration
		previousSample = (short) predictedSample;
		previousIndex = index;

		/*
		 * return the new ADPCM code
		 */
		byte codeAsByte = (byte) (code & 0x0F);
		return codeAsByte;
	}
}
