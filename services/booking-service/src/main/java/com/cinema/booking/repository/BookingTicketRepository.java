package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingTicket;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingTicketRepository extends JpaRepository<BookingTicket, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BookingTicket t WHERE t.id = :id")
    Optional<BookingTicket> findByIdForUpdate(@Param("id") UUID id);

    Optional<BookingTicket> findByQrToken(String qrToken);
    List<BookingTicket> findAllByBookingIdOrderBySeatNumberAsc(UUID bookingId);
}
