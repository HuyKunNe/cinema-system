package com.cinema.payment.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
            @Index(name = "idx_payments_booking", columnList = "booking_id"),
            @Index(name = "idx_payments_user_created", columnList = "user_id, created_at"),
            @Index(
                    name = "idx_payments_status_hold_expiration",
                    columnList = "status, hold_expires_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_payments_booking_attempt",
                    columnNames = {"booking_id", "payment_attempt"}),
            @UniqueConstraint(
                    name = "uk_payments_source_event",
                    columnNames = {"source_event_id"}),
            @UniqueConstraint(
                    name = "uk_payments_provider_reference",
                    columnNames = {"provider", "provider_reference"})
        })
public class Payment extends BaseEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "payment_attempt", nullable = false, updatable = false)
    private Integer paymentAttempt;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "provider", nullable = false, updatable = false, length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status = PaymentStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 50)
    private RefundStatus refundStatus = RefundStatus.NOT_REQUESTED;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "hold_expires_at", nullable = false, updatable = false)
    private OffsetDateTime holdExpiresAt;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID sourceEventId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "correlation_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID correlationId;

    protected Payment() {}

    public Payment(
            UUID bookingId,
            UUID userId,
            int paymentAttempt,
            BigDecimal amount,
            String currency,
            String provider,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime requestedAt,
            UUID sourceEventId,
            UUID correlationId) {

        validateRequiredIdentifiers(bookingId, userId, sourceEventId, correlationId);

        validatePaymentAttempt(paymentAttempt);
        validateAmount(amount);
        validateCurrency(currency);
        validateProvider(provider);
        validateTimes(holdExpiresAt, requestedAt);

        this.bookingId = bookingId;
        this.userId = userId;
        this.paymentAttempt = paymentAttempt;
        this.amount = amount;
        this.currency = normalizeCode(currency);
        this.provider = normalizeCode(provider);
        this.status = PaymentStatus.RECEIVED;
        this.refundStatus = RefundStatus.NOT_REQUESTED;
        this.holdExpiresAt = holdExpiresAt;
        this.requestedAt = requestedAt;
        this.sourceEventId = sourceEventId;
        this.correlationId = correlationId;
    }

    private static void validateRequiredIdentifiers(
            UUID bookingId, UUID userId, UUID sourceEventId, UUID correlationId) {

        if (bookingId == null) {
            throw new ValidationException(PaymentErrorCode.BOOKING_ID_REQUIRED);
        }

        if (userId == null) {
            throw new ValidationException(PaymentErrorCode.USER_ID_REQUIRED);
        }

        if (sourceEventId == null) {
            throw new ValidationException(PaymentErrorCode.SOURCE_EVENT_ID_REQUIRED);
        }

        if (correlationId == null) {
            throw new ValidationException(PaymentErrorCode.CORRELATION_ID_REQUIRED);
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

    private static void validateProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        if (provider.trim().length() > 50) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }
    }

    private static void validateTimes(OffsetDateTime holdExpiresAt, OffsetDateTime requestedAt) {

        if (holdExpiresAt == null) {
            throw new ValidationException(PaymentErrorCode.HOLD_EXPIRATION_REQUIRED);
        }

        if (requestedAt == null) {
            throw new ValidationException(PaymentErrorCode.REQUESTED_AT_REQUIRED);
        }

        if (!holdExpiresAt.isAfter(requestedAt)) {
            throw new ValidationException(PaymentErrorCode.HOLD_EXPIRATION_INVALID);
        }
    }

    private static void validatePaymentAttempt(int paymentAttempt) {

        if (paymentAttempt <= 0) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ATTEMPT_INVALID);
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

    private static String normalizeCode(String value) {

        return value.trim().toUpperCase(Locale.ROOT);
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Integer getPaymentAttempt() {
        return paymentAttempt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public OffsetDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public boolean isReceived() {
        return status == PaymentStatus.RECEIVED;
    }

    public boolean isTerminal() {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.EXPIRED;
    }
}
