package com.cinema.common.outbox.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private AggregateType aggregateType;

    private UUID aggregateId;

    private String eventType;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private Integer retryCount;

    private OffsetDateTime createdAt;

    private OffsetDateTime publishedAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(

            UUID id,

            AggregateType aggregateType,

            UUID aggregateId,

            String eventType,

            String payload

    ) {

        this.id = id;

        this.aggregateType = aggregateType;

        this.aggregateId = aggregateId;

        this.eventType = eventType;

        this.payload = payload;

        this.status = OutboxStatus.PENDING;

        this.retryCount = 0;

        this.createdAt = OffsetDateTime.now();

    }

    public void markProcessing() {

        this.status = OutboxStatus.PROCESSING;

    }

    public void markSent() {

        this.status = OutboxStatus.SENT;

        this.publishedAt = OffsetDateTime.now();

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

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
