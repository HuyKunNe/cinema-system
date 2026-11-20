package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingPickupPass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingPickupPassRepository extends JpaRepository<BookingPickupPass, UUID> {
    Optional<BookingPickupPass> findByBookingId(UUID bookingId);

    Optional<BookingPickupPass> findByQrToken(String qrToken);
}
