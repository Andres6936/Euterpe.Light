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
    JOIN_STEREO,
    DUAL_CHANNEL,
    SINGLE_CHANNEL,
}
