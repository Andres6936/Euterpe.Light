package javazoom.jlme.decoder;

/**
 * Filtering applied to an audio signal before storage or transmission to
 * improve the signal-to-noise ratio at high frequencies.
 */
public enum Emphasis {
    NO_EMPHASIS,
    MICROSEC_EMPHASIS,
    RESERVED,
    CCITJ_J17
}
