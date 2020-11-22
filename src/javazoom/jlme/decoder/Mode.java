package javazoom.jlme.decoder;

/**
 * In Layer I and II the joint_stereo mode is intensity_stereo,
 * in Layer III it is intensity_stereo and/or ms_stereo.
 */
public enum Mode {

    /**
     * Mode, where two audio channels which form a stereo pair (left and right)
     * are encoded within one bit stream. The coding process is the same as for
     * the dual channel mode.
     */
    STEREO,

    /**
     * A mode of the audio coding algorithm using joint stereo coding, and the
     * joint stereo coding [audio]: Any method that exploits stereophonic
     * irrelevance or stereophonic redundancy.
     */
    JOIN_STEREO,

    /**
     * A mode, where two audio channels with independent programmeme contents
     * (e.g. bilingual) are encoded within one bit stream. The coding process
     * is the same as for the stereo mode.
     */
    DUAL_CHANNEL,
    SINGLE_CHANNEL,
}
