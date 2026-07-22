package com.cinema.common.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BaseEvent {

    UUID eventId();

    String eventType();

    OffsetDateTime createdAt();

}
