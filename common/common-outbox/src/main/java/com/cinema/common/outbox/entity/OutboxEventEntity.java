package com.cinema.common.outbox.entity;

import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 50)
    private AggregateType aggregateType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "aggregate_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false, length = 20)
    private String eventVersion;

    @Column(name = "topic", nullable = false, updatable = false, length = 100)
    private String topic;

    @Column(name = "partition_key", nullable = false, updatable = false, length = 100)
    private String partitionKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "correlation_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID correlationId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "causation_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID causationId;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "processing_owner", length = 150)
    private String processingOwner;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processing_expires_at")
    private OffsetDateTime processingExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected OutboxEventEntity() {}

    public OutboxEventEntity(
            UUID id,
            AggregateType aggregateType,
            UUID aggregateId,
            String eventType,
            String eventVersion,
            String topic,
            String partitionKey,
            OffsetDateTime occurredAt,
            UUID correlationId,
            UUID causationId,
            String payload,
            OffsetDateTime createdAt) {

        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
    }

    public void claim(
            String processingOwner,
            OffsetDateTime processingStartedAt,
            OffsetDateTime processingExpiresAt) {

        this.status = OutboxStatus.PROCESSING;
        this.processingOwner = processingOwner;
        this.processingStartedAt = processingStartedAt;
        this.processingExpiresAt = processingExpiresAt;
        this.lastError = null;
    }

    public boolean markSent(String expectedOwner, OffsetDateTime publishedAt) {

        if (!isOwnedBy(expectedOwner)) {
            return false;
        }

        this.status = OutboxStatus.SENT;
        this.publishedAt = publishedAt;
        this.nextAttemptAt = null;

        clearProcessingLease();

        return true;
    }

    public boolean markFailed(
            String expectedOwner, OffsetDateTime nextAttemptAt, String lastError) {

        if (!isOwnedBy(expectedOwner)) {
            return false;
        }

        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = truncateError(lastError);

        clearProcessingLease();

        return true;
    }

    public boolean canRetry(int maximumAttempts) {

        return retryCount < maximumAttempts;
    }

    public boolean isOwnedBy(String expectedOwner) {

        return status == OutboxStatus.PROCESSING
                && expectedOwner != null
                && expectedOwner.equals(processingOwner);
    }

    private void clearProcessingLease() {

        this.processingOwner = null;
        this.processingStartedAt = null;
        this.processingExpiresAt = null;
    }

    private static String truncateError(String error) {

        if (error == null || error.isBlank()) {
            return null;
        }

        String normalized = error.trim();

        return normalized.length() <= 2000 ? normalized : normalized.substring(0, 2000);
    }

    public UUID getId() {

        return id;
    }

    public AggregateType getAggregateType() {

        return aggregateType;
    }

    public UUID getAggregateId() {

        return aggregateId;
    }

    public String getEventType() {

        return eventType;
    }

    public String getEventVersion() {

        return eventVersion;
    }

    public String getTopic() {

        return topic;
    }

    public String getPartitionKey() {

        return partitionKey;
    }

    public OffsetDateTime getOccurredAt() {

        return occurredAt;
    }

    public UUID getCorrelationId() {

        return correlationId;
    }

    public UUID getCausationId() {

        return causationId;
    }

    public String getPayload() {

        return payload;
    }

    public OutboxStatus getStatus() {

        return status;
    }

    public Integer getRetryCount() {

        return retryCount;
    }

    public OffsetDateTime getNextAttemptAt() {

        return nextAttemptAt;
    }

    public String getLastError() {

        return lastError;
    }

    public String getProcessingOwner() {

        return processingOwner;
    }

    public OffsetDateTime getProcessingStartedAt() {

        return processingStartedAt;
    }

    public OffsetDateTime getProcessingExpiresAt() {

        return processingExpiresAt;
    }

    public OffsetDateTime getCreatedAt() {

        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {

        return publishedAt;
    }
}
