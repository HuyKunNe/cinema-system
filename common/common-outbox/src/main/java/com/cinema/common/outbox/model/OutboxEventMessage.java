package com.cinema.common.outbox.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.common.kafka.event.BaseEvent;

public record OutboxEventMessage(

        UUID eventId,

        String eventType,

        OffsetDateTime createdAt,

        OutboxEventPayload payload

) implements BaseEvent {

}
