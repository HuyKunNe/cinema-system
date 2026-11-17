package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingTicket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingTicketRepository extends JpaRepository<BookingTicket, UUID> {
    java.util.Optional<BookingTicket> findByQrToken(String qrToken);
    List<BookingTicket> findAllByBookingIdOrderBySeatNumberAsc(UUID bookingId);
}
