package pl.kingdomcore.utils;

import java.util.OptionalDouble;
import java.util.OptionalLong;

public final class NumberParser {
    private NumberParser() {
    }

    public static OptionalDouble positiveDouble(String raw) {
        try {
            double value = Double.parseDouble(raw);
            return value > 0 ? OptionalDouble.of(value) : OptionalDouble.empty();
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    public static OptionalLong positiveLong(String raw) {
        try {
            long value = Long.parseLong(raw);
            return value > 0 ? OptionalLong.of(value) : OptionalLong.empty();
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
