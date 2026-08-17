package com.cinema.common.logging.constant;

public final class LoggingConstants {

    public static final String CORRELATION_ID = "correlationId";

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final int CORRELATION_ID_MAX_LENGTH = 100;

    public static final String REQUEST_ID = "requestId";

    private LoggingConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
