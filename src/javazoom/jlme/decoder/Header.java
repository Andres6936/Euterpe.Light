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
 * Description of the Class
 *
 * @author micah
 */
public final class Header {

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
     * Section 2.4.2.3 Header
     * <p>
     * The first 32 bits (four bytes) are header information which is common to all layers.
     */
    final void read_header(BitStream stream) throws IOException {
        int headerstring;
        int channel_bitrate;
        boolean sync = false;
        do {
            headerstring = stream.syncHeader(syncmode);

            assert verifySyncWord(headerstring);
            assert verifyAlgorithm(headerstring);
            assert verifyPaddingBitFor44SamplingFrequency(headerstring);

            System.out.println("Header String: " + Integer.toBinaryString(headerstring));
            System.out.println("SynWord: " + verifySyncWord(headerstring));
            System.out.println("Algorithm: " + verifyAlgorithm(headerstring));
            System.out.println("Layer: " + getLayerUsed(headerstring));
            System.out.println("Redundancy Added: " + isRedundancyAdded(headerstring));
            System.out.println("Bit Rate Index: " + getBitRateIndex(headerstring));
            System.out.println("Sampling Frequency: " + getSamplingFrequency(headerstring));
            System.out.println("Padding Bit: " + isPaddingBit(headerstring));
            System.out.println("Private Bit: " + isPrivateBit(headerstring));
            System.out.println("Mode: " + getMode(headerstring).name());
            System.out.println("Mode Extension: " + getModeExtension(headerstring));
            System.out.println("Copyright: " + isCopyright(headerstring));
            System.out.println("Is Original: " + isOriginalOrHome(headerstring));
            System.out.println("Emphasis: " + getEmphasis(headerstring).name());

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
            calFrameSize();
            // read framedata: Rest the 4 bytes of header
            stream.read_frame_data(frameLengthInBytes - 4);
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
    private void calFrameSize() {
        frameLengthInBytes = (144 * bitrate / sampleFrequency) + (paddingBit ? 1 : 0);
        nSlots = frameLengthInBytes - ((h_mode == SINGLE_CHANNEL) ? 17 : 32) - ((h_protection_bit != 0) ? 0 : 2) - 4;
    }

    // Getters

    public int getFrameLengthInBytes() {
        return frameLengthInBytes;
    }

    public Layer getLayer() {
        return layer;
    }
}
