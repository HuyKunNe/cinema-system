package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingPrintSimulation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingPrintSimulationRepository
        extends JpaRepository<BookingPrintSimulation, UUID> {
    Optional<BookingPrintSimulation> findByBookingId(UUID bookingId);

    boolean existsByActorIdAndIdempotencyKey(UUID actorId, String idempotencyKey);
}
