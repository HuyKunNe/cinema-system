package com.cinema.booking.entity;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
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

        this.id = requireValue(id, BookingErrorCode.PROCESSED_EVENT_ID_REQUIRED);

        this.eventId = requireValue(eventId, BookingErrorCode.EVENT_ID_REQUIRED);

        this.consumerName = requireText(consumerName, BookingErrorCode.CONSUMER_NAME_REQUIRED);

        this.eventType = requireText(eventType, BookingErrorCode.EVENT_TYPE_REQUIRED);

        this.eventVersion = requireText(eventVersion, BookingErrorCode.EVENT_VERSION_REQUIRED);

        this.processedAt = requireValue(processedAt, BookingErrorCode.PROCESSED_AT_REQUIRED);
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

    private static String requireText(String value, ErrorCode errorCode) {

        if (value == null || value.isBlank()) {
            throw new ValidationException(errorCode);
        }

        return value.trim();
    }

    private static <T> T requireValue(T value, ErrorCode errorCode) {

        if (value == null) {
            throw new ValidationException(errorCode);
        }

        return value;
    }
}
