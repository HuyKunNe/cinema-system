package com.cinema.inventory.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "show_seats")
public class ShowSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 50)
    private SeatType seatType;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

    @Column(name = "held_by_booking_id", columnDefinition = "BINARY(16)")
    private UUID heldByBookingId;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    protected ShowSeat() {
    }

    public ShowSeat(
            Showtime showtime,
            Seat seat,
            BigDecimal price) {

        validatePrice(price);

        this.showtime = showtime;
        this.seat = seat;
        this.seatNumber = seat.getSeatNumber();
        this.seatType = seat.getSeatType();
        this.price = price;
        this.status = seat.isActive()
                ? ShowSeatStatus.AVAILABLE
                : ShowSeatStatus.UNAVAILABLE;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    void assignShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public Seat getSeat() {
        return seat;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ShowSeatStatus getStatus() {
        return status;
    }

    public UUID getHeldByBookingId() {
        return heldByBookingId;
    }

    public OffsetDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void changePrice(BigDecimal price) {
        validatePrice(price);
        this.price = price;
    }

    public void hold(
            UUID bookingId,
            OffsetDateTime expiresAt,
            OffsetDateTime now) {

        if (bookingId == null) {
            throw new ValidationException(InventoryErrorCode.BOOKING_ID_REQUIRED);
        }

        if (expiresAt == null || now == null) {
            throw new ValidationException(InventoryErrorCode.HOLD_EXPIRATION_AND_CURRENT_TIME_REQUIRED);
        }

        if (!expiresAt.isAfter(now)) {
            throw new ValidationException(InventoryErrorCode.INVALID_HOLD_EXPIRATION);
        }

        if (status != ShowSeatStatus.AVAILABLE) {
            throw new ConflictException(InventoryErrorCode.ONLY_AVAILABLE_SEATS_CAN_BE_HELD);
        }

        status = ShowSeatStatus.HELD;
        heldByBookingId = bookingId;
        holdExpiresAt = expiresAt;
    }

    public void book(UUID bookingId) {
        requireHeldBy(bookingId);

        status = ShowSeatStatus.BOOKED;
        clearHold();
    }

    public void release(UUID bookingId) {
        requireHeldBy(bookingId);

        status = ShowSeatStatus.AVAILABLE;
        clearHold();
    }

    public void releaseExpiredHold(OffsetDateTime now) {
        if (status != ShowSeatStatus.HELD) {
            throw new ConflictException(InventoryErrorCode.ONLY_HELD_SEATS_CAN_BE_RELEASED);
        }

        if (now == null) {
            throw new ValidationException(InventoryErrorCode.CURRENT_TIME_REQUIRED);
        }

        if (holdExpiresAt == null || holdExpiresAt.isAfter(now)) {
            throw new ConflictException(InventoryErrorCode.SEAT_HOLD_NOT_EXPIRED);
        }

        status = ShowSeatStatus.AVAILABLE;
        clearHold();
    }

    public void makeUnavailable() {
        status = ShowSeatStatus.UNAVAILABLE;
        clearHold();
    }

    public void makeAvailable() {
        status = ShowSeatStatus.AVAILABLE;
        clearHold();
    }

    public boolean isHeldBy(UUID bookingId) {
        return status == ShowSeatStatus.HELD
                && bookingId != null
                && bookingId.equals(heldByBookingId);
    }

    public boolean hasExpired(OffsetDateTime now) {
        return status == ShowSeatStatus.HELD
                && holdExpiresAt != null
                && now != null
                && !holdExpiresAt.isAfter(now);
    }

    private void requireHeldBy(UUID bookingId) {
        if (!isHeldBy(bookingId)) {
            throw new IllegalStateException(
                    "Seat is not held by booking " + bookingId);
        }
    }

    private void clearHold() {
        heldByBookingId = null;
        holdExpiresAt = null;
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Seat price must be zero or greater");
        }
    }
}
