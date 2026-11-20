package com.cinema.booking.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_pickup_passes")
public class BookingPickupPass extends BaseEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @Column(name = "qr_token", nullable = false, updatable = false, length = 43)
    private String qrToken;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    protected BookingPickupPass() {}

    public BookingPickupPass(UUID bookingId, String qrToken, OffsetDateTime expiresAt) {
        this.bookingId = bookingId;
        this.qrToken = qrToken;
        this.expiresAt = expiresAt;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getQrPayload() {
        return "cinema-booking:v1:" + qrToken;
    }

    @Override
    public String toString() {
        return "BookingPickupPass[bookingId=" + bookingId + ", qrToken=REDACTED]";
    }
}
