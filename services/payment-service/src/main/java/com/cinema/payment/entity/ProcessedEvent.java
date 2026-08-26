package com.cinema.payment.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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

        this.id = requireValue(id, PaymentErrorCode.PROCESSED_EVENT_ID_REQUIRED);

        this.eventId = requireValue(eventId, PaymentErrorCode.EVENT_ID_REQUIRED);

        this.consumerName = requireText(consumerName, PaymentErrorCode.CONSUMER_NAME_REQUIRED);

        this.eventType = requireText(eventType, PaymentErrorCode.EVENT_TYPE_REQUIRED);

        this.eventVersion = requireText(eventVersion, PaymentErrorCode.EVENT_VERSION_REQUIRED);

        this.processedAt = requireValue(processedAt, PaymentErrorCode.PROCESSED_AT_REQUIRED);
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
}
