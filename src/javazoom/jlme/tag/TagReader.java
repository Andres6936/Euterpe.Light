package javazoom.jlme.tag;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Read the content of ID3v2 tag.
 * <br><br>
 * <p>
 * The ID3v2 offers a flexible way of storing information about an audio file
 * within itself to determine its origin and contents. The information may be
 * technical information, such as equalisation curves, as well as related meta
 * information, such as title, performer, copyright etc.
 * <br><br>
 * <p>
 * The tag consists of a header, frames and optional padding. A field is a
 * piece of information; one value, a string etc. A numeric string is a string
 * that consists of the characters 0-9 only.
 */
public class TagReader {

    private final byte[] dataTag;

    public TagReader(BufferedInputStream buffer) throws IOException {
        int totalBytesRead = 0;
        // The ID3v2 tag header, which should be the first information in the
        // file, is 10 bytes .
        byte[] header = new byte[10];
        totalBytesRead += buffer.read(header, 0, 10);
        verifyHeader(header);
        // Create a new array with the four bytes of size
        byte[] size = new byte[4];
        size[0] = header[6];
        size[1] = header[7];
        size[2] = header[8];
        size[3] = header[9];
        int sizeTag = getSizeTag(size);
        dataTag = new byte[sizeTag];
        totalBytesRead += buffer.read(dataTag, 0, sizeTag);
        assert totalBytesRead == sizeTag + 10;
    }

    /**
     * The first three bytes of the tag are always "ID3" to indicate that this
     * is an ID3v2 tag, directly followed by the two version bytes. The first
     * byte of ID3v2 version is it's major version, while the second byte is
     * its revision number.
     *
     * @param header Header frame of ID3 tag.
     */
    private void verifyHeader(final byte[] header) {
        assert header.length >= 5;
        assert header[0] == 'I' && header[1] == 'D' && header[2] == '3';
        assert header[3] == 3 && header[4] == 0;
        assert verifyFlags(header[5]);
    }

    private boolean verifyFlags(final byte flags) {
        System.out.println("Flags: " + Integer.toBinaryString(flags));
        return true;
    }

    private int getSizeTag(final byte[] size) {
        assert size.length == 4;
        System.out.println("Size[0]: " + Integer.toBinaryString(size[0]));
        System.out.println("Size[1]: " + Integer.toBinaryString(size[1]));
        System.out.println("Size[2]: " + Integer.toBinaryString(size[2]));
        System.out.println("Size[3]: " + Integer.toBinaryString(size[3]));
        return (size[0] << 24) | (size[1] << 16) | (size[2] << 8) | size[3];
    }
}
