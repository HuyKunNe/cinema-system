package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByUserIdAndClientRequestId(UUID userId, String clientRequestId);

    Optional<Booking> findByIdAndUserId(UUID bookingId, UUID userId);

    Page<Booking> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndClientRequestId(UUID userId, String clientRequestId);
}
