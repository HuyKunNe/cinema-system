package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentTransaction;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findAllByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);

    Optional<PaymentTransaction> findByProviderAndIdempotencyKey(
            String provider, String idempotencyKey);

    Optional<PaymentTransaction> findByProviderAndProviderEventId(
            String provider, String providerEventId);

    Optional<PaymentTransaction> findByProviderAndProviderReference(
            String provider, String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT transaction
            FROM PaymentTransaction transaction
            WHERE transaction.id = :transactionId
            """)
    Optional<PaymentTransaction> findByIdForUpdate(@Param("transactionId") UUID transactionId);
}
