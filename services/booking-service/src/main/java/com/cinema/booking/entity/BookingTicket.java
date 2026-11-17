package com.cinema.booking.entity;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "booking_tickets")
public class BookingTicket extends BaseEntity {
    private static final SecureRandom RANDOM = new SecureRandom();

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_seat_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingSeatId;

    @Column(name = "seat_number", nullable = false, updatable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false, updatable = false)
    private int capacity;

    @Column(name = "qr_token", nullable = false, updatable = false, length = 43)
    private String qrToken;

    @Column(nullable = false, length = 20)
    private String status = "ISSUED";

    @Column(name = "issuance_source", length = 30, updatable = false)
    private String issuanceSource;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "issued_by", updatable = false, columnDefinition = "BINARY(16)")
    private UUID issuedBy;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "checked_in_by", columnDefinition = "BINARY(16)")
    private UUID checkedInBy;

    @Column(name = "check_in_showtime_starts_at")
    private OffsetDateTime checkInShowtimeStartsAt;

    public OffsetDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void checkIn(
            OffsetDateTime now, UUID actor, OffsetDateTime startsAt) {
        if (!"ISSUED".equals(status)) throw new IllegalStateException("Ticket is not issued");
        Objects.requireNonNull(now);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(startsAt);
        if (now.isBefore(startsAt.minusMinutes(30)) || now.isAfter(startsAt.plusMinutes(15))) {
            throw new ConflictException(
                    BookingErrorCode.TICKET_CHECK_IN_WINDOW_CLOSED);
        }
        status = "USED";
        checkedInAt = now;
        checkedInBy = actor;
        checkInShowtimeStartsAt = startsAt;
    }

    protected BookingTicket() {}

    public BookingTicket(BookingSeat seat) {
        this(seat, null);
    }

    public BookingTicket(BookingSeat seat, UUID actor) {
        issuanceSource = actor == null ? "BOOKING_CONFIRMED_EVENT" : "ADMIN_RECOVERY";
        issuedBy = actor;
        if (!seat.hasCompletedSnapshot())
            throw new IllegalArgumentException("Ticket requires completed seat snapshot");
        bookingId = seat.getBookingId();
        bookingSeatId = seat.getId();
        seatNumber = seat.getSeatNumber();
        capacity = capacityFor(seat);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        qrToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static int capacityFor(BookingSeat seat) {
        return switch (seat.getSeatType()) {
            case "COUPLE" -> 2;
            case "STANDARD", "VIP", "ACCESSIBLE" -> 1;
            default -> throw new IllegalArgumentException("Unsupported ticket seat type");
        };
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getBookingSeatId() {
        return bookingSeatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }

    public String getQrPayload() {
        return "cinema-ticket:v1:" + qrToken;
    }
}
