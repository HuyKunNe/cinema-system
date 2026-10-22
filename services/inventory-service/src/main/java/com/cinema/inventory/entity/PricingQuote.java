package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_quotes")
public class PricingQuote extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(
            name = "showtime_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID showtimeId;

    @Column(name = "showtime_version", nullable = false, updatable = false)
    private Long showtimeVersion;

    @Column(
            name = "policy_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID policyId;

    @Column(name = "policy_version", nullable = false, updatable = false)
    private long policyVersion;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(
            name = "snapshot_json",
            nullable = false,
            updatable = false,
            columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(name = "consumed_by_booking_id", columnDefinition = "BINARY(16)")
    private UUID consumedByBookingId;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "reservation_expires_at")
    private OffsetDateTime reservationExpiresAt;

    protected PricingQuote() {}

    public PricingQuote(
            UUID userId,
            Showtime showtime,
            CinemaPricingPolicy policy,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {

        if (userId == null
                || showtime == null
                || policy == null
                || issuedAt == null
                || expiresAt == null
                || !expiresAt.isAfter(issuedAt)) {
            throw invalid();
        }

        this.userId = userId;
        this.showtimeId = showtime.getId();
        this.showtimeVersion = showtime.getVersion();
        this.policyId = policy.getId();
        this.policyVersion = policy.getPolicyVersion();
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public void sealSnapshot(String snapshotJson) {
        if (this.snapshotJson != null || snapshotJson == null || snapshotJson.isBlank()) {
            throw invalid();
        }

        this.snapshotJson = snapshotJson;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getShowtimeId() {
        return showtimeId;
    }

    public Long getShowtimeVersion() {
        return showtimeVersion;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public UUID getConsumedByBookingId() {
        return consumedByBookingId;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public OffsetDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public boolean isConsumed() {
        return consumedByBookingId != null;
    }

    public void consume(UUID bookingId, OffsetDateTime now, OffsetDateTime holdExpiresAt) {

        if (isConsumed()) {
            throw new ConflictException(InventoryErrorCode.QUOTE_ALREADY_CONSUMED);
        }

        if (bookingId == null
                || now == null
                || holdExpiresAt == null
                || now.isBefore(issuedAt)
                || !now.isBefore(expiresAt)
                || !holdExpiresAt.isAfter(now)
                || holdExpiresAt.isAfter(expiresAt)) {
            throw invalid();
        }

        consumedByBookingId = bookingId;
        consumedAt = now;
        reservationExpiresAt = holdExpiresAt;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.QUOTE_REQUEST_INVALID);
    }
}
