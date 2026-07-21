package com.cinema.common.core.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    public static String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
