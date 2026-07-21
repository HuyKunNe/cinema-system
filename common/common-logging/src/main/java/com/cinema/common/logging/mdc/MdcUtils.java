package com.cinema.common.logging.mdc;

import org.slf4j.MDC;

public final class MdcUtils {

    private MdcUtils() {
    }

    public static void put(String key, String value) {

        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }

    }

    public static String get(String key) {
        return MDC.get(key);
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    public static void clear() {
        MDC.clear();
    }

}
