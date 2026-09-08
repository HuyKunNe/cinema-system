package com.cinema.payment.entity;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
            @Index(name = "idx_payment_transactions_payment", columnList = "payment_id"),
            @Index(
                    name = "idx_payment_transactions_claim",
                    columnList = "status, processing_expires_at, requested_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_payment_transactions_provider_idempotency",
                    columnNames = {"provider", "idempotency_key"}),
            @UniqueConstraint(
                    name = "uk_payment_transactions_provider_event",
                    columnNames = {"provider", "provider_event_id"}),
            @UniqueConstraint(
                    name = "uk_payment_transactions_provider_reference",
                    columnNames = {"provider", "provider_reference"})
        })
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction {

    private static final int MAX_PROVIDER_LENGTH = 50;

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 200;

    private static final int MAX_PROCESSING_OWNER_LENGTH = 150;

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

    @Column(name = "provider", nullable = false, updatable = false, length = MAX_PROVIDER_LENGTH)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false, length = 50)
    private PaymentTransactionType transactionType;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentTransactionStatus status = PaymentTransactionStatus.READY;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = MAX_IDEMPOTENCY_KEY_LENGTH)
    private String idempotencyKey;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "provider_event_id", length = 255)
    private String providerEventId;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "processing_owner", length = 150)
    private String processingOwner;

    @Column(name = "processing_expires_at")
    private OffsetDateTime processingExpiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PaymentTransaction() {}

    public PaymentTransaction(
            UUID paymentId,
            String provider,
            PaymentTransactionType transactionType,
            int attemptNumber,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            OffsetDateTime requestedAt) {

        validatePaymentId(paymentId);
        validateProvider(provider);
        validateTransactionType(transactionType);
        validateAttemptNumber(attemptNumber);
        validateAmount(amount);
        validateCurrency(currency);
        validateIdempotencyKey(idempotencyKey);
        validateRequestedAt(requestedAt);

        this.paymentId = paymentId;
        this.provider = normalizeCode(provider);
        this.transactionType = transactionType;
        this.attemptNumber = attemptNumber;
        this.status = PaymentTransactionStatus.READY;
        this.amount = amount;
        this.currency = normalizeCode(currency);
        this.idempotencyKey = idempotencyKey.trim();
        this.requestedAt = requestedAt;
    }

    public boolean isClaimableAt(OffsetDateTime now) {

        if (now == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }

        return status == PaymentTransactionStatus.READY
                || (status == PaymentTransactionStatus.PROCESSING
                        && processingExpiresAt != null
                        && !processingExpiresAt.isAfter(now));
    }

    public void claim(String owner, OffsetDateTime claimedAt, OffsetDateTime leaseExpiresAt) {

        validateProcessingOwner(owner);
        validateProcessingLease(claimedAt, leaseExpiresAt);

        if (!isClaimableAt(claimedAt)) {
            throw new ConflictException(PaymentErrorCode.TRANSACTION_NOT_CLAIMABLE);
        }

        status = PaymentTransactionStatus.PROCESSING;
        processingOwner = owner.strip();
        processingExpiresAt = leaseExpiresAt;
    }

    public boolean isOwnedBy(String expectedOwner) {

        return status == PaymentTransactionStatus.PROCESSING
                && expectedOwner != null
                && expectedOwner.equals(processingOwner);
    }

    private static void validateProcessingOwner(String owner) {

        if (owner == null || owner.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_REQUIRED);
        }

        if (owner.strip().length() > MAX_PROCESSING_OWNER_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_INVALID);
        }
    }

    private static void validateProcessingLease(
            OffsetDateTime claimedAt, OffsetDateTime leaseExpiresAt) {

        if (claimedAt == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }

        if (leaseExpiresAt == null) {
            throw new ValidationException(PaymentErrorCode.PROCESSING_EXPIRATION_REQUIRED);
        }

        if (!leaseExpiresAt.isAfter(claimedAt)) {
            throw new ValidationException(PaymentErrorCode.PROCESSING_EXPIRATION_INVALID);
        }
    }

    public UUID getId() {

        return id;
    }

    public UUID getPaymentId() {

        return paymentId;
    }

    public String getProvider() {

        return provider;
    }

    public PaymentTransactionType getTransactionType() {

        return transactionType;
    }

    public Integer getAttemptNumber() {

        return attemptNumber;
    }

    public PaymentTransactionStatus getStatus() {

        return status;
    }

    public BigDecimal getAmount() {

        return amount;
    }

    public String getCurrency() {

        return currency;
    }

    public String getIdempotencyKey() {

        return idempotencyKey;
    }

    public String getProviderReference() {

        return providerReference;
    }

    public String getProviderEventId() {

        return providerEventId;
    }

    public String getFailureCode() {

        return failureCode;
    }

    public String getFailureMessage() {

        return failureMessage;
    }

    public OffsetDateTime getRequestedAt() {

        return requestedAt;
    }

    public OffsetDateTime getCompletedAt() {

        return completedAt;
    }

    public String getProcessingOwner() {

        return processingOwner;
    }

    public OffsetDateTime getProcessingExpiresAt() {

        return processingExpiresAt;
    }

    public OffsetDateTime getCreatedAt() {

        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {

        return updatedAt;
    }

    public boolean isReady() {

        return status == PaymentTransactionStatus.READY;
    }

    private static void validatePaymentId(UUID paymentId) {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }
    }

    private static void validateProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        if (provider.trim().length() > MAX_PROVIDER_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }
    }

    private static void validateTransactionType(PaymentTransactionType transactionType) {

        if (transactionType == null) {
            throw new ValidationException(PaymentErrorCode.TRANSACTION_TYPE_REQUIRED);
        }
    }

    private static void validateAttemptNumber(int attemptNumber) {

        if (attemptNumber <= 0) {
            throw new ValidationException(PaymentErrorCode.TRANSACTION_ATTEMPT_INVALID);
        }
    }

    private static void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_REQUIRED);
        }

        if (amount.signum() < 0) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_INVALID);
        }
    }

    private static void validateCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(PaymentErrorCode.CURRENCY_REQUIRED);
        }

        String normalized = currency.trim();

        if (normalized.length() != 3 || !normalized.chars().allMatch(Character::isLetter)) {

            throw new ValidationException(PaymentErrorCode.CURRENCY_INVALID);
        }
    }

    private static void validateIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        if (idempotencyKey.trim().length() > MAX_IDEMPOTENCY_KEY_LENGTH) {

            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_INVALID);
        }
    }

    private static void validateRequestedAt(OffsetDateTime requestedAt) {

        if (requestedAt == null) {
            throw new ValidationException(PaymentErrorCode.REQUESTED_AT_REQUIRED);
        }
    }

    private static String normalizeCode(String value) {

        return value.trim().toUpperCase(Locale.ROOT);
    }

    public boolean hasActiveLease(String expectedOwner, OffsetDateTime now) {

        if (now == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }

        return isOwnedBy(expectedOwner)
                && processingExpiresAt != null
                && processingExpiresAt.isAfter(now);
    }

    public void completeSuccessfully(
            String expectedOwner, String reference, OffsetDateTime completedAt) {

        requireOwnedBy(expectedOwner);
        validateProviderReference(reference);
        validateCompletionTime(completedAt);

        status = PaymentTransactionStatus.SUCCEEDED;
        providerReference = reference.strip();
        failureCode = null;
        failureMessage = null;
        this.completedAt = completedAt;

        clearProcessingLease();
    }

    public void completeFailed(
            String expectedOwner, String code, String message, OffsetDateTime completedAt) {

        requireOwnedBy(expectedOwner);
        validateFailureCode(code);
        validateCompletionTime(completedAt);

        status = PaymentTransactionStatus.FAILED;
        failureCode = code.strip();
        failureMessage = normalizeFailureMessage(message);
        this.completedAt = completedAt;

        clearProcessingLease();
    }

    public void markPendingProvider(
            String expectedOwner, String reference, String code, String message) {

        requireOwnedBy(expectedOwner);

        status = PaymentTransactionStatus.PENDING_PROVIDER;
        providerReference = normalizeProviderReference(reference);
        failureCode = normalizeFailureCode(code);
        failureMessage = normalizeFailureMessage(message);
        completedAt = null;

        clearProcessingLease();
    }

    private void requireOwnedBy(String expectedOwner) {

        if (!isOwnedBy(expectedOwner)) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_TRANSACTION_LEASE_NOT_OWNED);
        }
    }

    private void clearProcessingLease() {

        processingOwner = null;
        processingExpiresAt = null;
    }

    private static void validateCompletionTime(OffsetDateTime completionTime) {

        if (completionTime == null) {
            throw new ValidationException(PaymentErrorCode.CURRENT_TIME_REQUIRED);
        }
    }

    private static void validateProviderReference(String reference) {

        if (reference == null || reference.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED);
        }

        if (reference.strip().length() > 255) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }
    }

    private static String normalizeProviderReference(String reference) {

        if (reference == null || reference.isBlank()) {
            return null;
        }

        String normalizedReference = reference.strip();

        if (normalizedReference.length() > 255) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return normalizedReference;
    }

    private static void validateFailureCode(String code) {

        if (code == null || code.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
        }

        if (code.strip().length() > 100) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }
    }

    private static String normalizeFailureCode(String code) {

        if (code == null || code.isBlank()) {
            return null;
        }

        String normalizedCode = code.strip();

        if (normalizedCode.length() > 100) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return normalizedCode;
    }

    private static String normalizeFailureMessage(String message) {

        if (message == null || message.isBlank()) {
            return null;
        }

        String normalizedMessage = message.strip();

        if (normalizedMessage.length() > 500) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return normalizedMessage;
    }
}
