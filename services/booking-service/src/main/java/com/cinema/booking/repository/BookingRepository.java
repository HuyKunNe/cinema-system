package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByUserIdAndClientRequestId(UUID userId, String clientRequestId);

    Optional<Booking> findByIdAndUserId(UUID bookingId, UUID userId);

    Page<Booking> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndClientRequestId(UUID userId, String clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT booking
            FROM Booking booking
            WHERE booking.id = :bookingId
            """)
    Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);
}
