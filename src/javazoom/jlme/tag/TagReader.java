package javazoom.jlme.tag;

import java.io.BufferedInputStream;
import java.io.IOException;

public class TagReader {
    public int readTag(BufferedInputStream buffer) throws IOException {
        int totalBytesRead = 0;
        byte[] header = new byte[10];
        totalBytesRead += buffer.read(header, 0, 10);
        verifyHeader(header);
        return totalBytesRead;
    }

    private void verifyHeader(final byte[] header) {
        assert header.length == 10;
        assert header[0] == 'I' && header[1] == 'D' && header[2] == '3';
        assert header[3] == 3 && header[4] == 0;
        assert verifyFlags(header[5]);
        // Create a new array with the four bytes of size
        byte[] size = new byte[4];
        size[0] = header[6];
        size[1] = header[7];
        size[2] = header[8];
        size[3] = header[9];
        assert verifySize(size);
    }

    private boolean verifyFlags(final byte flags) {

    }

    private boolean verifySize(final byte[] size) {
        assert size.length == 4;
    }
}
