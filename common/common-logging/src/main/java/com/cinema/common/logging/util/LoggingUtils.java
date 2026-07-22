package com.cinema.common.logging.util;

public final class LoggingUtils {

    private LoggingUtils() {
    }

    public static String elapsed(long start) {
        return (System.currentTimeMillis() - start) + " ms";
    }

}
