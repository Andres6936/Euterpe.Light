package javazoom.jlme.decoder;

/**
 * Filtering applied to an audio signal before storage or transmission to
 * improve the signal-to-noise ratio at high frequencies.
 *
 * @implNote The term "reserved" when used in the clauses defining the coded
 * bit stream indicates that the value may be used in the future for ISO/IEC
 * defined extensions.
 */
public enum Emphasis {
    NO_EMPHASIS,
    MICROSEC_EMPHASIS,
    RESERVED,
    CCITJ_J17
}
