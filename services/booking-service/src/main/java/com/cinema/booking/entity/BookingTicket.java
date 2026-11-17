package com.cinema.booking.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.SecureRandom;
import java.util.Base64;
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

    protected BookingTicket() {}

    public BookingTicket(BookingSeat seat) {
        if (!seat.hasCompletedSnapshot())
            throw new IllegalArgumentException("Ticket requires completed seat snapshot");
        bookingId = seat.getBookingId();
        bookingSeatId = seat.getId();
        seatNumber = seat.getSeatNumber();
        capacity =
                switch (seat.getSeatType()) {
                    case "COUPLE" -> 2;
                    case "STANDARD", "VIP", "ACCESSIBLE" -> 1;
                    default -> throw new IllegalArgumentException("Unsupported ticket seat type");
                };
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        qrToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
