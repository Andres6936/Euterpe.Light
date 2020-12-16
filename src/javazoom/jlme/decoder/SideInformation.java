package javazoom.jlme.decoder;

/**
 * The side information part of the frame consists of information needed to
 * decode the main data. The size depends on the encoded channel mode.
 * If it is a single channel bitstream the size will be 17 bytes, if not,
 * 32 bytes are allocated.
 * <br><br>
 * <p>
 * There’s lots of information in the side info. Most of the bits describe
 * how the main data should be parsed, but there are also some parameters
 * saved here used by other parts of the decoder.
 */
public class SideInformation {
    public int main_data_begin;
    public int private_bits;
    public final Channel[] ch = new Channel[2];

    public SideInformation() {
        ch[0] = new Channel();
        ch[1] = new Channel();
    }

    public SideInformation(final byte[] buffer) {
        assert buffer.length == 17 || buffer.length == 32;

        if (buffer.length == 17) {
            // Reserve space for get 16 bits
            byte[] reserve = new byte[2];
            // Copy the first two bytes (16 bits) from the buffer
            reserve[0] = buffer[0];
            reserve[1] = buffer[1];
            // Get the first 8 bits
            int mainDataBegin = (reserve[0] << 8) | (reserve[1]);
            // Clear the unused bits, 16 (bit set) - 9 (bit used) = 7 (bit unused)
            mainDataBegin = mainDataBegin >>> 7;
            // Get the 5 bits of use private, the offset is 9 + 5 = 14 (Second bit, already get)
            // The bits of interest are marked with x, the other with _
            // The form of actual private bits is: _xxxxx__
            // Needed clear the bits without that not are of us interest
            byte privateBits = reserve[1];
            privateBits = (byte) (privateBits << 1);
            privateBits = (byte) (privateBits >>> 3);
        } else {
            // Reserve space for get 16 bits
            byte[] reserve = new byte[2];
            // Copy the first two bytes (16 bits) from the buffer
            reserve[0] = buffer[0];
            reserve[1] = buffer[1];
            // Get the first 8 bits
            // It is important to note that the byte values
            // in this code section can become negative
            // (signed), therefore it is important to perform an operation
            // extra of bitshifting (bits & 0xFF) to get your
            // byte-shaped representation and avoid promotion to int
            // which makes Java automatically.
            // Reference: https://stackoverflow.com/q/50980248
            int mainDataBegin = ((reserve[0] & 0xFF) << 8) | reserve[1] & 0xFF;
            // Clear the unused bits, 16 (bit set) - 9 (bit used) = 7 (bit unused)
            mainDataBegin = mainDataBegin >>> 7;
            System.out.println("Data Begin: " + mainDataBegin);
        }
    }
}
