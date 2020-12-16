package javazoom.jlme.decoder;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Many computer users know that an MP3 are made up of several “frames”,
 * consecutive blocks of data. While important for unpacking the bit stream,
 * frames are not fundamental and cannot be decoded individually. For this
 * case, what is usually called a frame we call a physical frame, while
 * we call a block of data that can actually be decoded a logical frame, or
 * simply just a frame.
 * <p>
 * A logical frame has many parts: it has a 4 byte header easily
 * distinguishable from other data in the bit stream, it has 17 or 32 bytes
 * known as side information, and a few hundred bytes of main data.
 */
public class Frame extends AbstractFrame {

    /**
     * The algorithm used.
     */
    private final Layer layer;

    /**
     * The encoded channel mode.
     */
    private final Mode mode;

    /**
     * The side information.
     */
    private final SideInformation sideInformation;

    /**
     * The bitrate index in hz.
     */
    private final int bitrate;

    /**
     * The sync word or header string.
     */
    private final int headerString;

    /**
     * The sampling frequency in hz.
     */
    private final int sampleFrequency;

    /**
     * The size of each frame when is compressed.
     */
    private int frameLengthInBytes = 0;

    /**
     * Determine if it present the padding bit.
     */
    private final boolean paddingBit;

    /**
     * Buffer that store the data frame.
     */
    private final byte[] dataFrame;

    /**
     * Buffer that store the data side information.
     */
    private final byte[] dataSideInformation;

    public Frame(BufferedInputStream buffer) throws IOException {
        headerString = getHeader(buffer);

        assert verifySyncWord(headerString);
        assert verifyAlgorithm(headerString);
        assert verifyPaddingBitFor44SamplingFrequency(headerString);

        layer = getLayerUsed(headerString);
        // Convert the value of kHz to hz.
        bitrate = getBitRateIndex(headerString) * 1_000;
        // Convert the value of kHz to hz.
        sampleFrequency = (int) getSamplingFrequency(headerString) * 1_000;
        paddingBit = isPaddingBit(headerString);
        mode = getMode(headerString);

        // The side information part of the frame consists of information
        // needed to decode the main data. The size depends on the encoded
        // channel mode. If it is a single channel bitstream the size will
        // be 17 bytes, if not, 32 bytes are allocated.
        final int sizeSideInformation = (mode == Mode.SINGLE_CHANNEL ? 17 : 32);
        dataSideInformation = new byte[sizeSideInformation];
        assert buffer.read(dataSideInformation, 0, sizeSideInformation) == sizeSideInformation;

        sideInformation = new SideInformation(dataSideInformation);
        determineFrameLengthInBytes();

        // Rest 4 bytes: Because the frame length include the header (4 bytes of header),
        // and the header has been read yet, and too rest the size of side information
        // block, because it too has been read.
        assert frameLengthInBytes != 0 : "The frame length not has been initialized.";
        final int sizeOfDataFrame = frameLengthInBytes - (4 + sizeSideInformation);
        dataFrame = new byte[sizeOfDataFrame];
        assert buffer.read(dataFrame, 0, sizeOfDataFrame) == sizeOfDataFrame;
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

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String toString() {
        // The average of each invocation to toString is 461 characters.
        StringBuilder representation = new StringBuilder(500);

        representation.append("\nHeader String: ").append(Integer.toBinaryString(headerString));
        representation.append("\nSynWord: ").append(verifySyncWord(headerString));
        representation.append("\nAlgorithm: ").append(verifyAlgorithm(headerString));
        representation.append("\nLayer: ").append(getLayerUsed(headerString));
        representation.append("\nRedundancy Added: ").append(isRedundancyAdded(headerString));
        representation.append("\nBit Rate Index: ").append(getBitRateIndex(headerString));
        representation.append("\nSampling Frequency: ").append(getSamplingFrequency(headerString));
        representation.append("\nPadding Bit: ").append(isPaddingBit(headerString));
        representation.append("\nPrivate Bit: ").append(isPrivateBit(headerString));
        representation.append("\nMode: ").append(getMode(headerString).name());
        representation.append("\nMode Extension: ").append(getModeExtension(headerString));
        representation.append("\nCopyright: ").append(isCopyright(headerString));
        representation.append("\nIs Original: ").append(isOriginalOrHome(headerString));
        representation.append("\nEmphasis: ").append(getEmphasis(headerString).name());

        return representation.toString();
    }
}
