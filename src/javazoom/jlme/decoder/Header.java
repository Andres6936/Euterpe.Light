/*
 *  JLayerME is a JAVA library that decodes/plays/converts MPEG 1/2 Layer 3.
 *  Project Homepage: http://www.javazoom.net/javalayer/javalayerme.html.
 *  Copyright (C) JavaZOOM 1999-2005.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *---------------------------------------------------------------------------
 *  19 Aug 2004 - Konstantin Belous 
 *  Added code for MPEG 2 support 
 *  (used the mpglib (http://ftp.tu-clausthal.de/pub/unix/audio/mpg123) as the source for changes).  
 *---------------------------------------------------------------------------
 */
package javazoom.jlme.decoder;


import java.io.IOException;
import java.util.OptionalInt;

/**
 * The 4-byte header stores some properties about the audio signal, most
 * importantly the sample rate and the channel mode (mono, stereo etc).
 * The information in the header is useful both for media player software,
 * and for decoding the audio. Note that the header does not store many
 * parameters used by the decoder, e.g. how audio samples should be
 * reconstructed, those parameters are stored elsewhere.
 */
public final class Header extends IFrame {

    /**
     * The algorithm used.
     */
    private Layer layer;

    /**
     * The bitrate index in hz.
     */
    private int bitrate = 0;

    /**
     * The header string or sync header.
     */
    private int headerstring = 0;

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

    public final static int[][] frequencies =
            {{22050, 24000, 16000, 1},
                    {44100, 48000, 32000, 1}};

    /**
     * Constant for MPEG-1 version
     */
    public final static int MPEG1 = 1;

    /**
     * Constant for MPEG-2 version
     */
    public final static int MPEG2 = 0;

    /**
     * Description of the Field
     */
    public final static int JOINT_STEREO = 1;

    /**
     * Description of the Field
     */
    public final static int SINGLE_CHANNEL = 3;

    /**
     * Description of the Field
     */
    public final static int FOURTYEIGHT = 1;

    /**
     * Description of the Field
     */
    public final static int THIRTYTWO = 2;

    public static int nSlots;
    private static int h_layer, h_protection_bit, h_bitrate_index, h_padding_bit, h_mode_extension;
    private static int h_version;
    private static int h_mode;
    private static int h_sample_frequency;
    static byte syncmode = BitStream.INITIAL_SYNC;


    public int version() {
        return h_version;
    }

    public int sample_frequency() {
        return h_sample_frequency;
    }

    public int frequency() {
        return frequencies[h_version][h_sample_frequency];
    }

    public int mode() {
        return h_mode;
    }

    public int slots() {
        return nSlots;
    }

    public int mode_extension() {
        return h_mode_extension;
    }

    /**
     * Section 2.4.2.3 Header
     * <p>
     * The first 32 bits (four bytes) are header information which is common to all layers.
     */
    final void read_header(BitStream stream) throws IOException {
        int channel_bitrate;
        boolean sync = false;
        do {
            headerstring = stream.findAndReturnSyncHeader(syncmode);

            assert verifySyncWord(headerstring);
            assert verifyAlgorithm(headerstring);
            assert verifyPaddingBitFor44SamplingFrequency(headerstring);

            layer = getLayerUsed(headerstring);
            // Convert the value of kHz to hz.
            bitrate = getBitRateIndex(headerstring) * 1_000;
            // Convert the value of kHz to hz.
            sampleFrequency = (int) getSamplingFrequency(headerstring) * 1_000;
            paddingBit = isPaddingBit(headerstring);

            if (syncmode == BitStream.INITIAL_SYNC) {
                h_version = ((headerstring >>> 19) & 1);
                if ((h_sample_frequency = ((headerstring >>> 10) & 3)) == 3) {
                    return;
                }
            }
            h_layer = 4 - (headerstring >>> 17) & 3;
            // E.B Fix.
            //h_protection_bit = 0;
            h_protection_bit = (headerstring >>> 16) & 1;
            // End.
            h_bitrate_index = (headerstring >>> 12) & 0xF;
            h_padding_bit = (headerstring >>> 9) & 1;
            h_mode = ((headerstring >>> 6) & 3);
            h_mode_extension = (headerstring >>> 4) & 3;
            int h_intensity_stereo_bound;
            if (h_mode == JOINT_STEREO) {
                h_intensity_stereo_bound = (h_mode_extension << 2) + 4;
            } else {
                h_intensity_stereo_bound = 0;
            }
            int h_number_of_subbands;
            if (h_layer == 1) {
                h_number_of_subbands = 32;
            } else {
                channel_bitrate = h_bitrate_index;
                // calculate bitrate per channel:
                if (h_mode != SINGLE_CHANNEL) {
                    if (channel_bitrate == 4) {
                        channel_bitrate = 1;
                    } else {
                        channel_bitrate -= 4;
                    }
                }
                if (h_version == MPEG2) {
                    h_number_of_subbands = 30;
                } else if ((channel_bitrate == 1) || (channel_bitrate == 2)) {
                    if (h_sample_frequency == THIRTYTWO) {
                        h_number_of_subbands = 12;
                    } else {
                        h_number_of_subbands = 8;
                    }
                } else if ((h_sample_frequency == FOURTYEIGHT) || ((channel_bitrate >= 3) && (channel_bitrate <= 5))) {
                    h_number_of_subbands = 27;
                } else {
                    h_number_of_subbands = 30;
                }
            }
            if (h_intensity_stereo_bound > h_number_of_subbands) {
                h_intensity_stereo_bound = h_number_of_subbands;
            }
            // calculate framesize and nSlots
            determineFrameLengthInBytes();
            // read framedata: Rest the 4 bytes of header
            stream.readFrameData(frameLengthInBytes - 4);
            if (stream.isSyncCurrentPosition(syncmode)) {
                if (syncmode == BitStream.INITIAL_SYNC) {
                    syncmode = BitStream.STRICT_SYNC;
                    stream.set_syncword(headerstring & 0xFFF80CC0);
                }
                sync = true;
            } else {
                stream.unreadFrame();
            }
        } while (!sync);
        stream.parse_frame();

        // E.B Fix
        if (h_protection_bit == 0) {
            // frame contains a crc checksum
            short checksum = (short) stream.readbits(16);
        }
        // End
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
        // Side information can either be 17 bytes if it is a single channel or 32 bytes if
        // it is a dual channel. Side information always immediately follows the
        // header. Basically, it contains all the relevant information to decode the main
        // data.
        // For example it contains the main data begin pointer, scale factor
        // selection information, Huffman table information for both the granules etc.
        nSlots = frameLengthInBytes - ((h_mode == SINGLE_CHANNEL) ? 17 : 32) - ((h_protection_bit != 0) ? 0 : 2) - 4;
    }

    // Override

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String toString() {
        // The average of each invocation to toString is 461 characters.
        StringBuilder representation = new StringBuilder(500);

        representation.append("\nHeader String: ").append(Integer.toBinaryString(headerstring));
        representation.append("\nSynWord: ").append(verifySyncWord(headerstring));
        representation.append("\nAlgorithm: ").append(verifyAlgorithm(headerstring));
        representation.append("\nLayer: ").append(getLayerUsed(headerstring));
        representation.append("\nRedundancy Added: ").append(isRedundancyAdded(headerstring));
        representation.append("\nBit Rate Index: ").append(getBitRateIndex(headerstring));
        representation.append("\nSampling Frequency: ").append(getSamplingFrequency(headerstring));
        representation.append("\nPadding Bit: ").append(isPaddingBit(headerstring));
        representation.append("\nPrivate Bit: ").append(isPrivateBit(headerstring));
        representation.append("\nMode: ").append(getMode(headerstring).name());
        representation.append("\nMode Extension: ").append(getModeExtension(headerstring));
        representation.append("\nCopyright: ").append(isCopyright(headerstring));
        representation.append("\nIs Original: ").append(isOriginalOrHome(headerstring));
        representation.append("\nEmphasis: ").append(getEmphasis(headerstring).name());

        return representation.toString();
    }

    // Getters

    public int getFrameLengthInBytes() {
        return frameLengthInBytes;
    }

    public Layer getLayer() {
        return layer;
    }
}
