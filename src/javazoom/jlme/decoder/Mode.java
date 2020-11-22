package javazoom.jlme.decoder;

/**
 * In Layer I and II the joint_stereo mode is intensity_stereo,
 * in Layer III it is intensity_stereo and/or ms_stereo.
 */
public enum Mode {
    STEREO,
    JOIN_STEREO,
    DUAL_CHANNEL,
    SINGLE_CHANNEL,
}
