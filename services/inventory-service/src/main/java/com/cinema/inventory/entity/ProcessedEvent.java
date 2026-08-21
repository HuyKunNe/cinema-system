package com.cinema.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_processed_events_event_consumer",
                        columnNames = {"event_id", "consumer_name"}))
public class ProcessedEvent {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, updatable = false, length = 100)
    private String consumerName;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false, length = 20)
    private String eventVersion;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(
            UUID id,
            UUID eventId,
            String consumerName,
            String eventType,
            String eventVersion,
            OffsetDateTime processedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.consumerName = requireText(consumerName, "consumerName");
        this.eventType = requireText(eventType, "eventType");
        this.eventVersion = requireText(eventVersion, "eventVersion");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    public UUID getId() {

        return id;
    }

    public UUID getEventId() {

        return eventId;
    }

    public String getConsumerName() {

        return consumerName;
    }

    public String getEventType() {

        return eventType;
    }

    public String getEventVersion() {

        return eventVersion;
    }

    public OffsetDateTime getProcessedAt() {

        return processedAt;
    }

    private static String requireText(String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }
}
