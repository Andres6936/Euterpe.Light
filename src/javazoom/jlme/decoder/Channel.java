package javazoom.jlme.decoder;

public class Channel {
    public final int[] scfsi = new int[4];
    public final GRInfo[] gr = new GRInfo[2];

    public Channel() {
        gr[0] = new GRInfo();
        gr[1] = new GRInfo();
    }
}
