package javazoom.jlme.decoder;

import java.util.OptionalInt;

public class Layer3 {
    public static OptionalInt getBitRateIndex(final int bits) throws IllegalArgumentException {
        // The all zero value indicates the 'free format' condition, in which a fixed
        // bitrate which does not need to be in the list can be used.
        if (bits == 0b0000) {
            // The free format indicate: Any bitrate other than the defined
            // bitrates that is less than the maximum valid bitrate for each
            // layer.
            return OptionalInt.empty();
        } else if (bits == 0b0001) {
            return OptionalInt.of(32);
        } else if (bits == 0b0010) {
            return OptionalInt.of(40);
        } else if (bits == 0b0011) {
            return OptionalInt.of(48);
        } else if (bits == 0b0100) {
            return OptionalInt.of(56);
        } else if (bits == 0b0101) {
            return OptionalInt.of(64);
        } else if (bits == 0b0110) {
            return OptionalInt.of(80);
        } else if (bits == 0b0111) {
            return OptionalInt.of(96);
        } else if (bits == 0b1000) {
            return OptionalInt.of(112);
        } else if (bits == 0b1001) {
            return OptionalInt.of(128);
        } else if (bits == 0b1010) {
            return OptionalInt.of(160);
        } else if (bits == 0b1011) {
            return OptionalInt.of(192);
        } else if (bits == 0b1100) {
            return OptionalInt.of(224);
        } else if (bits == 0b1101) {
            return OptionalInt.of(256);
        } else if (bits == 0b1110) {
            return OptionalInt.of(320);
        }

        throw new IllegalArgumentException("Sequence of bits not is recognized for the standard");
    }
}
