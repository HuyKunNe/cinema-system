package com.cinema.common.core.util;

public final class NumberUtils {

    private NumberUtils() {
    }

    public static boolean isPositive(long value) {
        return value > 0;
    }

    public static boolean isNegative(long value) {
        return value < 0;
    }

    public static Long defaultIfNull(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }
}
