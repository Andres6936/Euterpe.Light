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
}
