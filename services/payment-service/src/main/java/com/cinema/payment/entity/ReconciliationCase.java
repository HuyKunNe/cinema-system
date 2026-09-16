package com.cinema.payment.entity;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.exception.PaymentErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "reconciliation_cases",
        indexes = {
            @Index(name = "idx_reconciliation_cases_payment", columnList = "payment_id"),
            @Index(
                    name = "idx_reconciliation_cases_status_opened",
                    columnList = "status, opened_at"),
            @Index(
                    name = "idx_reconciliation_cases_provider_reference",
                    columnList = "provider, provider_reference")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_reconciliation_cases_transaction",
                    columnNames = "payment_transaction_id")
        })
@EntityListeners(AuditingEntityListener.class)
public class ReconciliationCase {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id = UuidGenerator.next();

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "payment_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "payment_transaction_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID paymentTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReconciliationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 100)
    private ReconciliationReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 100)
    private ReconciliationResolution resolution;

    @Column(name = "provider", nullable = false, updatable = false, length = 50)
    private String provider;

    @Column(name = "provider_reference", updatable = false, length = 255)
    private String providerReference;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_by_type", length = 50)
    private FinancialAuditActorType resolvedByType;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolution_reason", length = 500)
    private String resolutionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ReconciliationCase() {}

    public ReconciliationCase(
            UUID paymentId,
            UUID paymentTransactionId,
            ReconciliationReason reason,
            String provider,
            String providerReference,
            OffsetDateTime openedAt) {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (paymentTransactionId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED);
        }

        if (reason == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_REASON_REQUIRED);
        }

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        if (provider.strip().length() > 50) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        if (openedAt == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }

        this.paymentId = paymentId;
        this.paymentTransactionId = paymentTransactionId;
        this.status = ReconciliationStatus.OPEN;
        this.reason = reason;
        this.resolution = null;
        this.provider = provider.strip().toUpperCase(Locale.ROOT);
        this.providerReference = normalize(providerReference, 255);
        this.openedAt = openedAt;
    }

    public void resolve(
            ReconciliationResolution resolution,
            FinancialAuditActorType actorType,
            String actorId,
            String reason,
            OffsetDateTime resolvedAt) {

        requireOpen();

        if (resolution == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_RESOLUTION_REQUIRED);
        }

        requireResolver(actorType, actorId, resolvedAt);

        this.status = ReconciliationStatus.RESOLVED;
        this.resolution = resolution;
        this.resolvedByType = actorType;
        this.resolvedBy = actorId.strip();
        this.resolutionReason = normalize(reason, 500);
        this.resolvedAt = resolvedAt;
    }

    public void reject(
            FinancialAuditActorType actorType,
            String actorId,
            String reason,
            OffsetDateTime resolvedAt) {

        requireOpen();
        requireResolver(actorType, actorId, resolvedAt);

        this.status = ReconciliationStatus.REJECTED;
        this.resolution = null;
        this.resolvedByType = actorType;
        this.resolvedBy = actorId.strip();
        this.resolutionReason = normalize(reason, 500);
        this.resolvedAt = resolvedAt;
    }

    private void requireOpen() {

        if (status != ReconciliationStatus.OPEN) {
            throw new ConflictException(PaymentErrorCode.RECONCILIATION_CASE_NOT_OPEN);
        }
    }

    private static void requireResolver(
            FinancialAuditActorType actorType, String actorId, OffsetDateTime resolvedAt) {

        if (actorType == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_TYPE_REQUIRED);
        }

        if (actorId == null || actorId.isBlank()) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_ID_REQUIRED);
        }

        if (actorId.strip().length() > 255) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_ID_INVALID);
        }

        if (resolvedAt == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }
    }

    private static String normalize(String value, int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_VALUE_INVALID);
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public ReconciliationReason getReason() {
        return reason;
    }

    public ReconciliationResolution getResolution() {
        return resolution;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public FinancialAuditActorType getResolvedByType() {
        return resolvedByType;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public String getResolutionReason() {
        return resolutionReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
