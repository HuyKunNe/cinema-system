package com.cinema.payment.entity;

import com.cinema.payment.provider.model.ProviderOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_provider_webhook_events",
        indexes =
                @Index(
                        name = "idx_payment_provider_webhook_events_transaction",
                        columnList = "payment_transaction_id, processed_at"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_payment_provider_webhook_events_provider_event",
                        columnNames = {"provider", "provider_event_id"}))
public class PaymentProviderWebhookEvent {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "payment_transaction_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID paymentTransactionId;

    @Column(name = "provider", nullable = false, updatable = false, length = 50)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, updatable = false, length = 255)
    private String providerEventId;

    @Column(name = "provider_reference", nullable = false, updatable = false, length = 255)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 50)
    private ProviderOutcome outcome;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected PaymentProviderWebhookEvent() {}

    public UUID getId() {
        return id;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public ProviderOutcome getOutcome() {
        return outcome;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
