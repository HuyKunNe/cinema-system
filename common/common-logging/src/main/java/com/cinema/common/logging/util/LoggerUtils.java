package com.cinema.common.logging.util;

import org.slf4j.Logger;

public final class LoggerUtils {

    private LoggerUtils() {
    }

    public static void info(Logger logger, String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(message, args);
        }
    }

    public static void warn(Logger logger, String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, args);
        }
    }

    public static void error(Logger logger, String message, Object... args) {
        logger.error(message, args);
    }

}
