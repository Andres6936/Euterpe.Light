package javazoom.jlme.decoder;

/**
 * The side information is 17 bytes for mono, 32 bytes otherwise. There’s
 * lots of information in the side info. Most of the bits describe how the
 * main data should be parsed, but there are also some parameters saved
 * here used by other parts of the decoder.
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
