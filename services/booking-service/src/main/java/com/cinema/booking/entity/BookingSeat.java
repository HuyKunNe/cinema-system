package com.cinema.booking.entity;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "booking_seats",
        indexes = {@Index(name = "idx_booking_seats_booking", columnList = "booking_id")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_booking_seats_booking_seat",
                    columnNames = {"booking_id", "showtime_id", "seat_number"})
        })
@EntityListeners(AuditingEntityListener.class)
public class BookingSeat {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id = UuidGenerator.next();

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "inventory_seat_id", columnDefinition = "BINARY(16)")
    private UUID inventorySeatId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "showtime_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID showtimeId;

    @Column(name = "seat_number", nullable = false, updatable = false, length = 20)
    private String seatNumber;

    @Column(name = "seat_type", length = 50)
    private String seatType;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BookingSeat() {}

    public BookingSeat(UUID bookingId, UUID showtimeId, String seatNumber) {

        validateBookingId(bookingId);
        validateShowtimeId(showtimeId);
        validateSeatNumber(seatNumber);

        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seatNumber = normalizeSeatNumber(seatNumber);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getInventorySeatId() {
        return inventorySeatId;
    }

    public UUID getShowtimeId() {
        return showtimeId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean hasCompletedSnapshot() {
        return inventorySeatId != null && seatType != null && price != null;
    }

    public void completeSnapshot(UUID inventorySeatId, String seatType, BigDecimal price) {

        if (hasAnySnapshotValue()) {
            throw new ConflictException(BookingErrorCode.SEAT_SNAPSHOT_ALREADY_COMPLETED);
        }

        validateInventorySeatId(inventorySeatId);
        validateSeatType(seatType);
        validatePrice(price);

        this.inventorySeatId = inventorySeatId;
        this.seatType = seatType.trim().toUpperCase(Locale.ROOT);
        this.price = price;
    }

    private boolean hasAnySnapshotValue() {
        return inventorySeatId != null || seatType != null || price != null;
    }

    private static String normalizeSeatNumber(String seatNumber) {

        return seatNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static void validateBookingId(UUID bookingId) {
        if (bookingId == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }
    }

    private static void validateShowtimeId(UUID showtimeId) {
        if (showtimeId == null) {
            throw new ValidationException(BookingErrorCode.SHOWTIME_ID_REQUIRED);
        }
    }

    private static void validateSeatNumber(String seatNumber) {

        if (seatNumber == null || seatNumber.isBlank()) {
            throw new ValidationException(BookingErrorCode.SEAT_NUMBER_REQUIRED);
        }
    }

    private static void validateInventorySeatId(UUID inventorySeatId) {

        if (inventorySeatId == null) {
            throw new ValidationException(BookingErrorCode.INVENTORY_SEAT_ID_REQUIRED);
        }
    }

    private static void validateSeatType(String seatType) {
        if (seatType == null || seatType.isBlank()) {
            throw new ValidationException(BookingErrorCode.SEAT_TYPE_REQUIRED);
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new ValidationException(BookingErrorCode.INVALID_SEAT_PRICE);
        }
    }
}
