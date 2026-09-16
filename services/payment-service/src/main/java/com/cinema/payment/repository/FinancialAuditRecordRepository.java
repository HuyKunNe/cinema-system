package com.cinema.payment.repository;

import com.cinema.payment.entity.FinancialAuditRecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinancialAuditRecordRepository extends JpaRepository<FinancialAuditRecord, UUID> {

    List<FinancialAuditRecord> findAllByPaymentIdOrderByOccurredAtAscIdAsc(UUID paymentId);
}
