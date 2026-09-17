package com.cinema.payment.repository;

import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.model.ReconciliationCaseLockReference;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationCaseRepository extends JpaRepository<ReconciliationCase, UUID> {

    Optional<ReconciliationCase> findByPaymentTransactionId(UUID paymentTransactionId);

    List<ReconciliationCase> findAllByPaymentIdOrderByOpenedAtAscIdAsc(UUID paymentId);

    List<ReconciliationCase> findAllByStatusOrderByOpenedAtAscIdAsc(ReconciliationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT reconciliationCase
            FROM ReconciliationCase reconciliationCase
            WHERE reconciliationCase.id = :reconciliationCaseId
            """)
    Optional<ReconciliationCase> findByIdForUpdate(
            @Param("reconciliationCaseId") UUID reconciliationCaseId);

    @Query(
            """
            SELECT new com.cinema.payment.model.ReconciliationCaseLockReference(
                reconciliationCase.paymentId,
                reconciliationCase.paymentTransactionId
            )
            FROM ReconciliationCase reconciliationCase
            WHERE reconciliationCase.id = :reconciliationCaseId
            """)
    Optional<ReconciliationCaseLockReference> findLockReferenceById(
            @Param("reconciliationCaseId") UUID reconciliationCaseId);
}
