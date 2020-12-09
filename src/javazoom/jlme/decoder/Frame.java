package javazoom.jlme.decoder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.OptionalInt;

public class Frame {

    /**
     * The algorithm used.
     */
    private Layer layer;

    /**
     * The bitrate index in hz.
     */
    private int bitrate = 0;

    /**
     * The sampling frequency in hz.
     */
    private int sampleFrequency = 0;

    /**
     * The size of each frame when is compressed.
     */
    private int frameLengthInBytes = 0;

    /**
     * Determine if it present the padding bit.
     */
    private boolean paddingBit = false;

    /**
     * Buffer that store the data frame.
     */
    private final byte[] dataFrame;

    public Frame(BufferedInputStream buffer) throws IOException {
        int headerString = getHeader(buffer);

        assert verifySyncWord(headerString);
        assert verifyAlgorithm(headerString);
        assert verifyPaddingBitFor44SamplingFrequency(headerString);

        System.out.println("Header String: " + Integer.toBinaryString(headerString));
        System.out.println("SynWord: " + verifySyncWord(headerString));
        System.out.println("Algorithm: " + verifyAlgorithm(headerString));
        System.out.println("Layer: " + getLayerUsed(headerString));
        System.out.println("Redundancy Added: " + isRedundancyAdded(headerString));
        System.out.println("Bit Rate Index: " + getBitRateIndex(headerString));
        System.out.println("Sampling Frequency: " + getSamplingFrequency(headerString));
        System.out.println("Padding Bit: " + isPaddingBit(headerString));
        System.out.println("Private Bit: " + isPrivateBit(headerString));
        System.out.println("Mode: " + getMode(headerString).name());
        System.out.println("Mode Extension: " + getModeExtension(headerString));
        System.out.println("Copyright: " + isCopyright(headerString));
        System.out.println("Is Original: " + isOriginalOrHome(headerString));
        System.out.println("Emphasis: " + getEmphasis(headerString).name());

        layer = getLayerUsed(headerString);
        // Convert the value of kHz to hz.
        bitrate = getBitRateIndex(headerString) * 1_000;
        // Convert the value of kHz to hz.
        sampleFrequency = (int) getSamplingFrequency(headerString) * 1_000;
        paddingBit = isPaddingBit(headerString);

        determineFrameLengthInBytes();

        dataFrame = new byte[frameLengthInBytes];
        assert buffer.read(dataFrame, 0, frameLengthInBytes) == frameLengthInBytes;
    }

    /**
     * @return The first 32 bits (four bytes) are header information which is common to all layers.
     */
    public int getHeader(BufferedInputStream bufferStream) {
        final byte[] buffer = new byte[4];

        try {
            assert bufferStream.read(buffer, 0, 4) == 4;
        } catch (IOException exception) {
            System.err.println("Read of header not is possible.");
        }

        return (buffer[0] << 24) + (buffer[1] << 16) + (buffer[2] << 8) + buffer[3];
    }

    /**
     * The term 'syncword' is a 12-bit code embedded in the audio bit stream
     * that identifies the start of a frame.
     *
     * @param header The header information, common to all layers.
     * @return True if the SyncWord is the bit string '1111 1111 1111'.
     */
    private boolean verifySyncWord(final int header) {
        // The first 12 bits of header contain the bit string.
        // The shift right is 20, because 32 - 12 = 20
        // An 'int' have 32 bits in Java.
        return (header >>> 20) == 0b1111_1111_1111;
    }

    /**
     * One bit to indicate the ID of the algorithm. Equals '1' for MPEG audio, '0' is reserved.
     *
     * @param header The header information, common to all layers.
     * @return True if the ID is '1', false if the ID is '0'.
     */
    private boolean verifyAlgorithm(final int header) {
        // The bit 13 of header information have the information of ID.
        // The shift right is 19, because 32 - 13 = 19
        // An 'int' have 32 bits in Java.
        // To make the bitshift, not is possible get the bit 13 in terms of '0' and '1'
        //  this always will be content information of other bit that not are part of
        //  ID, for hence, is important make the operation bit AND for verify only the
        //  bit that we needed.
        // The statement INTEGER 'BIT OPERATION' INTEGER in Java return a INTEGER.
        //  For hence, is needed convert it value INTEGER to BOOLEAN
        return ((header >>> 19) & 0b0001) == 1;
    }

    /**
     * Determine which layer is used.
     *
     * @param header The header information, common to all layers.
     * @return The layer that is used.
     */
    private Layer getLayerUsed(final int header) {
        // Layer - 2 bits to indicate which layer is used, according to the following table.
        //  - "11" Layer I
        //  - "10" Layer II
        //  - "01" Layer III
        //  - "00" reserved
        // The bit 14 and 15 have the information of layer.
        int result = header >>> 17;
        // For get the result in terms of '11', '01', '10', or '00' is needed move bits.
        // Move 30 bits to left for clear the part left of bits.
        result = result << 30;
        // Reset the position of bits.
        result = result >>> 30;

        if (result == 0b0011) {
            return Layer.LAYER1;
        } else if (result == 0b0010) {
            return Layer.LAYER2;
        } else if (result == 0b0001) {
            return Layer.LAYER3;
        }

        // Formally, the last value possible is return the layer Reserved.
        return Layer.RESERVED;
    }

    /**
     * Indicate whether redundancy has been added in the audio bitstream to facilitate
     * error detection and concealment.
     *
     * @param header The header information, common to all layers.
     * @return True if bit is '0', false if the bit is '1'
     */
    private boolean isRedundancyAdded(final int header) {
        // Equals '1' if no redundancy has been added, '0' if redundancy has been added.
        return ((header >>> 16) & 0b0001) == 0;
    }

    /**
     * The rate at which the compressed bit stream is delivered from the
     * storage medium to the input of a decoder.
     *
     * @param header The header information, common to all layers.
     * @return The bit rate index in kHz, the which indicates the total bitrate irrespective of the
     * mode (stereo, joint_stereo, dual_channel, single_channel).
     */
    private int getBitRateIndex(final int header) {
        int result = header >>> 12;
        result = result << 28;
        result = result >>> 28;

        try {
            // The bit_rate_index is an index to a table, which is different for the
            // different Layers.
            OptionalInt rateIndex = Layer3.getBitRateIndex(result);
            if (rateIndex.isPresent()) {
                return rateIndex.getAsInt();
            } else {
                return 0;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Not is possible determine the bit rate index.");
            return -1;
        }
    }

    /**
     * @param header The header information, common to all layers.
     * @return the sampling frequency in kHz.
     * @implNote A reset of the decoder is required to change the sampling rate.
     */
    private float getSamplingFrequency(final int header) {
        int result = header >>> 10;
        result = result << 30;
        result = result >>> 30;

        if (result == 0b0000) {
            return 44.1f;
        } else if (result == 0b0001) {
            return 48f;
        } else if (result == 0b0010) {
            return 32f;
        } else {
            // Only value possible: 0b0011: return the sampling frequency Reserved.
            return -1f;
        }
    }

    /**
     * The padding is a method to adjust the average length in time of an audio
     * frame to the duration of the corresponding PCM samples, by conditionally
     * adding a slot to the audio frame.
     * <p>
     * A slot is an elementary part in the bit stream. In Layer I a slot equals
     * four bytes, in Layers II and III one byte.
     *
     * @param header The header information, common to all layers.
     * @return True if have a frame that contains an additional slot to adjust
     * the mean bitrate to the sampling frequency.
     * @implNote Padding is only necessary with a sampling frequency of 44.1kHz.
     */
    private boolean isPaddingBit(final int header) {
        // if this bit equals '1' the frame contains an additional slot to
        // adjust the mean bitrate to the sampling frequency, otherwise this
        // bit will be '0'.
        return ((header >>> 9) & 0b0001) == 1;
    }

    /**
     * The ISO 11172-3 say: Padding is only necessary with a sampling frequency
     * of 44.1kHz.
     *
     * @param header The header information, common to all layers.
     * @return True if the invariant is satisfied, false in otherwise.
     * @implNote Reference: ISO 11172-3 Pag. 22 - Section padding_bit
     */
    private boolean verifyPaddingBitFor44SamplingFrequency(final int header) {
        // For sampling frequencies different of 44.1 kHz, the padding bit
        // must be false (it is 0).
        if (getSamplingFrequency(header) != 44.1f) {
            // Verify that padding bit is 0 (it is false).
            // Equivalent to isPaddingBit(...) == false;
            return !isPaddingBit(header);
        }

        // The value of padding bit for sampling frequency equal to 44.1 can be
        // 0 or 1. (it is false or true).
        return true;
    }

    /**
     * @param header The header information, common to all layers.
     * @return True if the private bit is present, false otherwise.
     * @implNote bit for private use. This bit will not be used in the future
     * by ISO. Reference: ISO 11172-3 Pag. 22 - Section private_bit
     */
    private boolean isPrivateBit(final int header) {
        return ((header >>> 8) & 0b0001) == 1;
    }

    /**
     * @param header The header information, common to all layers.
     * @return the mode.
     */
    private Mode getMode(final int header) {
        int result = header >>> 6;
        result = result << 30;
        result = result >>> 30;

        if (result == 0b0000) {
            return Mode.STEREO;
        } else if (result == 0b0001) {
            return Mode.JOIN_STEREO;
        } else if (result == 0b0010) {
            return Mode.DUAL_CHANNEL;
        } else {
            // Only value possible: 0b0011
            return Mode.SINGLE_CHANNEL;
        }
    }

    /**
     * @param header The header information, common to all layers.
     * @return the mode extension.
     */
    private int getModeExtension(final int header) {
        int result = header >>> 4;
        result = result << 30;
        result = result >>> 30;

        if (result == 0b0000) {
            return 4;
        } else if (result == 0b0001) {
            return 8;
        } else if (result == 0b0010) {
            return 12;
        } else {
            // Only value posssible: 0b0011
            return 16;
        }
    }

    /**
     * @param header The header information, common to all layers.
     * @return True if is copyright protected, false otherwise.
     */
    private boolean isCopyright(final int header) {
        // If this bit equals '0' there is no copyright on the coded bitstream,
        // '1' means copyright protected.
        return ((header >>> 3) & 0b0001) == 1;
    }

    /**
     * @param header The header information, common to all layers.
     * @return True if the bitstream is an original, false otherwise.
     */
    private boolean isOriginalOrHome(final int header) {
        // If this bit equals '0' if the bitstream is a copy, '1' if it is an original
        return ((header >>> 2) & 0b0001) == 1;
    }

    /**
     * @param header The header information, common to all layers.
     * @return the emphasis.
     */
    private Emphasis getEmphasis(final int header) {
        int result = header << 30;
        result = result >>> 30;

        if (result == 0b0000) {
            return Emphasis.NO_EMPHASIS;
        } else if (result == 0b0001) {
            return Emphasis.MICROSEC_EMPHASIS;
        } else if (result == 0b0010) {
            return Emphasis.RESERVED;
        } else {
            // Only value possible: 0b0011
            return Emphasis.CCITJ_J17;
        }
    }

    /**
     * All MP3 files are divided into smaller fragments called frames. Each
     * frame stores 1152 audio samples and lasts for 26 ms. This means that the
     * frame rate will be around 38 fps. In addition a frame is subdivided into
     * two granules each containing 576 samples. Since the bitrate determines
     * the size of each sample, increasing the bitrate will also increase the
     * size of the frame. The size is also depending on the sampling frequency
     * according to following formula:
     * <p><br>
     * <p>
     * (144 * bitrate / sampleFrequency) + padding [bytes]
     * <p><br>
     * <p>
     * Padding refers to a special bit allocated in the beginning of the frame.
     * It is used in some frames to exactly satisfy the bitrate requirements.
     * If the padding bit is set the frame is padded with 1 byte. Note that the
     * frame size is an integer: Ex: 144*128000/44100 = 417
     * <br><br>
     * <p>
     * - Precondition: The format is MPEG 1 Layer 3.
     *
     * @apiNote Only support to MPEG 1 Layer 3.
     */
    private void determineFrameLengthInBytes() {
        frameLengthInBytes = (144 * bitrate / sampleFrequency) + (paddingBit ? 1 : 0);
    }
}
