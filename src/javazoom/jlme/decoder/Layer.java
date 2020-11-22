package javazoom.jlme.decoder;

/**
 * One of the levels in the coding hierarchy of the audio system defined in
 * this part of the CD.
 *
 * @implNote The term "reserved" when used in the clauses defining the coded
 * bit stream indicates that the value may be used in the future for ISO/IEC
 * defined extensions.
 */
public enum Layer {
    LAYER1,
    LAYER2,
    LAYER3,
    RESERVED,
}
