package com.cinema.common.kafka.constant;

public final class KafkaConstants {

    private KafkaConstants() {
    }

    public static final String HEADER_EVENT_ID = "X-Event-Id";

    public static final String HEADER_EVENT_TYPE = "X-Event-Type";

    public static final String HEADER_EVENT_VERSION = "X-Event-Version";

    public static final String DEFAULT_GROUP_ID = "cinema-system";

    public static final String DEAD_LETTER_SUFFIX = ".DLT";

}
