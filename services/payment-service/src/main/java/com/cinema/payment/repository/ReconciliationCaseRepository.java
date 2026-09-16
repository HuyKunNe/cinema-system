package com.cinema.payment.repository;

import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.ReconciliationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationCaseRepository extends JpaRepository<ReconciliationCase, UUID> {

    Optional<ReconciliationCase> findByPaymentTransactionId(UUID paymentTransactionId);

    List<ReconciliationCase> findAllByPaymentIdOrderByOpenedAtAscIdAsc(UUID paymentId);

    List<ReconciliationCase> findAllByStatusOrderByOpenedAtAscIdAsc(ReconciliationStatus status);
}
