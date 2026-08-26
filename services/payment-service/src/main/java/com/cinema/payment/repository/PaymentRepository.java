package com.cinema.payment.repository;

import com.cinema.payment.entity.Payment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingIdAndPaymentAttempt(UUID bookingId, Integer paymentAttempt);

    Optional<Payment> findBySourceEventId(UUID sourceEventId);

    Optional<Payment> findByIdAndUserId(UUID paymentId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT payment
            FROM Payment payment
            WHERE payment.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(@Param("paymentId") UUID paymentId);
}
