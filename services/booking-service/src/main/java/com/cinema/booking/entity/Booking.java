package com.cinema.booking.entity;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
            @Index(name = "idx_bookings_user_created", columnList = "user_id, created_at"),
            @Index(name = "idx_bookings_status_expiration", columnList = "status, expires_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_bookings_user_client_request",
                    columnNames = {"user_id", "client_request_id"})
        })
public class Booking extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "showtime_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID showtimeId;

    @Column(name = "client_request_id", nullable = false, length = 100)
    private String clientRequestId;

    @Column(
            name = "request_fingerprint",
            nullable = false,
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)")
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    protected Booking() {}

    public Booking(
            UUID userId,
            UUID showtimeId,
            String clientRequestId,
            String requestFingerprint,
            OffsetDateTime expiresAt,
            OffsetDateTime now) {

        validateUserId(userId);
        validateShowtimeId(showtimeId);
        validateClientRequestId(clientRequestId);
        validateRequestFingerprint(requestFingerprint);
        validateExpiration(expiresAt, now);

        this.userId = userId;
        this.showtimeId = showtimeId;
        this.clientRequestId = clientRequestId.trim();
        this.requestFingerprint = requestFingerprint;
        this.status = BookingStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getShowtimeId() {
        return showtimeId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    private static void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException(BookingErrorCode.USER_ID_REQUIRED);
        }
    }

    private static void validateShowtimeId(UUID showtimeId) {
        if (showtimeId == null) {
            throw new ValidationException(BookingErrorCode.SHOWTIME_ID_REQUIRED);
        }
    }

    private static void validateClientRequestId(String clientRequestId) {

        if (clientRequestId == null || clientRequestId.isBlank()) {

            throw new ValidationException(BookingErrorCode.CLIENT_REQUEST_ID_REQUIRED);
        }
    }

    private static void validateExpiration(OffsetDateTime expiresAt, OffsetDateTime now) {

        if (expiresAt == null || now == null) {
            throw new ValidationException(BookingErrorCode.EXPIRATION_REQUIRED);
        }

        if (!expiresAt.isAfter(now)) {
            throw new ValidationException(BookingErrorCode.INVALID_EXPIRATION);
        }
    }

    private static void validateRequestFingerprint(String requestFingerprint) {

        if (requestFingerprint == null || requestFingerprint.length() != 64) {

            throw new ValidationException(BookingErrorCode.REQUEST_FINGERPRINT_REQUIRED);
        }
    }
}
