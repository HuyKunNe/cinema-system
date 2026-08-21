package com.cinema.common.outbox.model;

import com.cinema.common.kafka.event.BaseEvent;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEventMessage(
        UUID eventId,
        UUID aggregateId,
        String aggregateType,
        String eventType,
        String eventVersion,
        OffsetDateTime occurredAt,
        UUID correlationId,
        UUID causationId,
        JsonNode payload)
        implements BaseEvent {

    @Override
    public OffsetDateTime createdAt() {

        return occurredAt;
    }
}
