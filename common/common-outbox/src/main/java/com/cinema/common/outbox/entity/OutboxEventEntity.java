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
        this.createdAt = createdAt;
    }

    public void markProcessing() {

        this.status = OutboxStatus.PROCESSING;
    }

    public void markSent(OffsetDateTime publishedAt) {

        this.status = OutboxStatus.SENT;
        this.publishedAt = publishedAt;
    }

    public void markFailed() {

        this.status = OutboxStatus.FAILED;
        this.retryCount++;
    }

    public boolean canRetry() {

        return retryCount < 5;
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

    public OffsetDateTime getCreatedAt() {

        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {

        return publishedAt;
    }
}
