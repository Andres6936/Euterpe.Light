package javazoom.jlme.decoder;

public class GRInfo {
    public int part2_3_length;
    public int big_values;
    public int global_gain;
    public int scalefac_compress;
    public int window_switching_flag;
    public int block_type;
    public int mixed_block_flag;
    public final int[] table_select = new int[3];
    public final int[] subblock_gain = new int[3];
    public int region0_count;
    public int region1_count;
    public int preflag;
    public int scalefac_scale;
    public int count1table_select;
}
