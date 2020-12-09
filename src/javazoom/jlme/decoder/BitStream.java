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
 *
 */
package javazoom.jlme.decoder;

import javazoom.jlme.tag.TagReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * The <code>Bistream</code> class is responsible for parsing an MPEG audio bitstream. <b>REVIEW:</b> much of the parsing currently occurs in the
 * various decoders. This should be moved into this class and associated inner classes.
 *
 * @author micah
 */
public final class BitStream {

    static final byte INITIAL_SYNC = 0;
    static final byte STRICT_SYNC = 1;
    private final static int BUFFER_INT_SIZE = 433;
    private final static int[] bitmask = {0,
            0x00000001, 0x00000003, 0x00000007, 0x0000000F,
            0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF,
            0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF,
            0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF,
            0x0001FFFF};

    private static PushBackStream source;
    private static final int[] framebuffer = new int[BUFFER_INT_SIZE];
    private static int framesize;
    private static final byte[] frame_bytes = new byte[BUFFER_INT_SIZE * 4];
    private static int wordpointer;
    private static int bitindex;
    private static int syncword;
    private static boolean single_ch_mode;
    private final static Header header = new Header();
    private final static byte[] syncbuf = new byte[4];

    static boolean sync;

    /**
     * Representation in bytes of mp3.
     */
    private final BufferedInputStream bufferByte;

    private final BufferedInputStream bufferCopy;

    public BitStream(InputStream in, InputStream copy) {
        bufferByte = new BufferedInputStream(in);
        bufferCopy = new BufferedInputStream(copy);

        try {
            TagReader tagReader = new TagReader(bufferCopy);
        } catch (IOException e) {
            e.printStackTrace();
        }

        source = new PushBackStream(in, 512);
        closeFrame();
        Header.syncmode = INITIAL_SYNC;
    }

    static int read;

    public final boolean isSyncCurrentPosition(int syncmode) throws IOException {
        if ((read = source.read(syncbuf, 0, 4)) > 0) {  //if(read>=0){
            source.unread(syncbuf, 0, read);
            if (read == 4) {
                headerstring = ((syncbuf[0] << 24) & 0xFF000000) | ((syncbuf[1] << 16) & 0x00FF0000) | ((syncbuf[2] << 8) & 0x0000FF00) | ((syncbuf[3]) & 0x000000FF);
                return isSyncMark(headerstring, syncmode, syncword);
            }
        }

        return true;
    }

    /**
     * @return The first 32 bits (four bytes) are header information which is common to all layers.
     */
    public int getHeader() {
        final byte[] buffer = new byte[4];

        try {
            assert bufferByte.read(buffer, 0, 4) == 4;
        } catch (IOException exception) {
            System.err.println("Read of header not is possible.");
        }

        return (buffer[0] << 24) + (buffer[1] << 16) + (buffer[2] << 8) + buffer[3];
    }

    /**
     * Gets the syncMark attribute of the Bitstream object
     *
     * @param headerstring  Description of Parameter
     * @param syncmode      Description of Parameter
     * @param word          Description of Parameter
     * @return The syncMark value
     */
    public final boolean isSyncMark(int headerstring, int syncmode, int word) {
        if (syncmode == INITIAL_SYNC) {
            sync = ((headerstring & 0xFFF00000) == 0xFFF00000);
        } else {
            sync = ((headerstring & 0xFFF80C00) == word) && (((headerstring & 0x000000C0) == 0x000000C0) == single_ch_mode);
        }
        // filter out invalid sample rate
        if (sync)
            if (sync = (((headerstring >>> 10) & 3) != 3))
                if (sync = (((headerstring >>> 17) & 3) != 0))
                    sync = (((headerstring >>> 19) & 3) != 1);

        return sync;
    }


    static int sum, returnvalue;

    public final int readbits(int num) {
        sum = bitindex + num;
        if (sum <= 32) {
            returnvalue = (framebuffer[wordpointer] >>> (32 - sum)) & bitmask[num];
            if ((bitindex += num) == 32) {
                bitindex = 0;
                wordpointer++;
            }

            return returnvalue;
        }

        returnvalue = (((framebuffer[wordpointer++] & 0x0000FFFF) << 16) & 0xFFFF0000) | (((framebuffer[wordpointer] & 0xFFFF0000) >>> 16) & 0x0000FFFF);
        returnvalue >>>= 48 - sum;
        returnvalue &= bitmask[num];
        bitindex = sum - 32;
        return returnvalue;
    }

    /**
     * Description of the Method
     *
     * @throws IOException Description of Exception
     */
    public void close() throws IOException {
        source.close();
    }

    static boolean readOnlyOnce = false;

    /**
     * Reads and parses the next frame from the input source.
     *
     * @return the Header describing details of the frame read, or null if the end of the stream has been reached.
     * @throws IOException Description of Exception
     */
    public Header readFrame() throws IOException {
        //if (framesize == -1) {
        header.read_header(this);
        //}
        if (!readOnlyOnce) {
            readHeaders();
            readOnlyOnce = true;
        }

        return header;
    }

    private void readHeaders() {
        final int frameSize = header.getFrameLengthInBytes();
        ArrayList<Frame> frames = new ArrayList<>();
        try {
            // The total of frames in the audio should be a number divisible
            assert bufferCopy.available() % frameSize == 0;
            while (bufferCopy.available() > 0) {
                frames.add(new Frame(bufferCopy.readNBytes(frameSize)));
            }
        } catch (IOException exception) {
            System.out.println("Frame not is possible read bytes.");
        }
        assert frames.size() == 758;
    }

    /**
     * Unreads the bytes read from the frame.
     */
    // REVIEW: add new error codes for this.
    public final void unreadFrame() throws IOException {
        if (wordpointer == -1 && bitindex == -1 && (framesize > 0)) {
            source.unread(frame_bytes, 0, framesize);
        }
    }

    /**
     * Description of the Method
     */
    public void closeFrame() {
        framesize = wordpointer = bitindex = -1;
    }

    /**
     * Set the word we want to sync the header to. In Big-Endian byte order
     *
     * @param syncword0 Description of Parameter
     */
    final void set_syncword(int syncword0) {
        syncword = syncword0 & 0xFFFFFF3F;
        single_ch_mode = ((syncword0 & 0x000000C0) == 0x000000C0);
    }

    static int headerstring;

    public int findAndReturnSyncHeader(byte syncmode) throws IOException {
        byte[] buffer = new byte[4];

        if (source.read(buffer, 0, 3) != 3) {
            return -1;
        }

        headerstring = buffer[0] << 16 & 0xff0000 | buffer[1] << 8 & 0xff00 | buffer[2] & 0xff;

        do {
            headerstring <<= 8;
            if (source.read(buffer, 3, 1) != 1) {
                return -1;
            }
            headerstring |= buffer[3] & 0xff;
        } while (!isSyncMark(headerstring, syncmode, syncword));

        return headerstring;
    }

    /**
     * Reads the data for the next frame. The frame is not parsed until parse frame is called.
     *
     * @param frameLength Description of Parameter
     * @throws IOException Description of Exception
     */
    final void readFrameData(final int frameLength) throws IOException {
        assert frameLength > 0;
        framesize = frameLength;
        wordpointer = bitindex = -1;
        source.read(frame_bytes, 0, frameLength);
    }

    static int b, k;
    static byte b0, b1, b2, b3;

    /**
     * Parses the data previously read with read_frame_data().
     */
    final void parse_frame() {
        // Convert Bytes read to int
        for (k = 0, b = 0; k < framesize; k += 4) {
            b0 = frame_bytes[k];
            if (k + 3 < framesize) {
                b3 = frame_bytes[k + 3];
                b2 = frame_bytes[k + 2];
                b1 = frame_bytes[k + 1];
            } else if (k + 2 < framesize) {
                b3 = 0;
                b2 = frame_bytes[k + 2];
                b1 = frame_bytes[k + 1];
            } else if (k + 1 < framesize) {
                b2 = b3 = 0;
                b1 = frame_bytes[k + 1];
            }
            framebuffer[b++] = ((b0 << 24) & 0xFF000000) | ((b1 << 16) & 0x00FF0000) | ((b2 << 8) & 0x0000FF00) | (b3 & 0x000000FF);
        }

        wordpointer = bitindex = 0;
    }

}
