package com.cinema.payment.entity;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.exception.PaymentErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "financial_audit_records",
        indexes = {
            @Index(
                    name = "idx_financial_audit_payment_occurred",
                    columnList = "payment_id, occurred_at"),
            @Index(
                    name = "idx_financial_audit_actor_occurred",
                    columnList = "actor_type, actor_id, occurred_at"),
            @Index(name = "idx_financial_audit_action_occurred", columnList = "action, occurred_at")
        })
public class FinancialAuditRecord {

    private static final int MAX_ACTOR_ID_LENGTH = 255;
    private static final int MAX_REASON_LENGTH = 500;
    private static final int MAX_METADATA_LENGTH = 2000;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 50)
    private FinancialAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, updatable = false, length = 50)
    private FinancialAuditActorType actorType;

    @Column(name = "actor_id", nullable = false, updatable = false, length = MAX_ACTOR_ID_LENGTH)
    private String actorId;

    @Column(name = "reason", updatable = false, length = MAX_REASON_LENGTH)
    private String reason;

    @Column(name = "metadata", updatable = false, length = MAX_METADATA_LENGTH)
    private String metadata;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "correlation_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    protected FinancialAuditRecord() {}

    public FinancialAuditRecord(
            UUID paymentId,
            FinancialAuditAction action,
            FinancialAuditActorType actorType,
            String actorId,
            String reason,
            String metadata,
            UUID correlationId,
            OffsetDateTime occurredAt) {

        validatePaymentId(paymentId);
        validateAction(action);
        validateActorType(actorType);
        validateActorId(actorId);
        validateCorrelationId(correlationId);
        validateOccurredAt(occurredAt);
        validateReason(reason);
        validateMetadata(metadata);

        this.paymentId = paymentId;
        this.action = action;
        this.actorType = actorType;
        this.actorId = actorId.strip();
        this.reason = normalizeOptional(reason);
        this.metadata = normalizeOptional(metadata);
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    private static void validatePaymentId(UUID paymentId) {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }
    }

    private static void validateAction(FinancialAuditAction action) {

        if (action == null) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTION_REQUIRED);
        }
    }

    private static void validateActorType(FinancialAuditActorType actorType) {

        if (actorType == null) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_TYPE_REQUIRED);
        }
    }

    private static void validateActorId(String actorId) {

        if (actorId == null || actorId.isBlank()) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_ID_REQUIRED);
        }

        if (actorId.strip().length() > MAX_ACTOR_ID_LENGTH) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_ID_INVALID);
        }
    }

    private static void validateCorrelationId(UUID correlationId) {

        if (correlationId == null) {
            throw new ValidationException(PaymentErrorCode.CORRELATION_ID_REQUIRED);
        }
    }

    private static void validateOccurredAt(OffsetDateTime occurredAt) {

        if (occurredAt == null) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_OCCURRED_AT_REQUIRED);
        }
    }

    private static void validateReason(String reason) {

        if (reason != null && reason.strip().length() > MAX_REASON_LENGTH) {

            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_REASON_INVALID);
        }
    }

    private static void validateMetadata(String metadata) {

        if (metadata != null && metadata.strip().length() > MAX_METADATA_LENGTH) {

            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_METADATA_INVALID);
        }
    }

    private static String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public FinancialAuditAction getAction() {
        return action;
    }

    public FinancialAuditActorType getActorType() {
        return actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public String getReason() {
        return reason;
    }

    public String getMetadata() {
        return metadata;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
