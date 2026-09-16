package com.cinema.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Transactional
class FinancialAuditRecordRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT =
            OffsetDateTime.parse("2026-09-16T09:00:00Z");

    private static final OffsetDateTime FIRST_AUDIT_AT =
            OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinancialAuditRecordRepository auditRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistFinancialAuditRecord() {

        Payment payment = persistPayment();

        FinancialAuditRecord audit =
                new FinancialAuditRecord(
                        payment.getId(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditActorType.USER,
                        "finance-user-1",
                        "Customer requested refund",
                        "channel=admin-api",
                        UuidGenerator.next(),
                        FIRST_AUDIT_AT);

        auditRepository.saveAndFlush(audit);

        UUID auditId = audit.getId();

        entityManager.clear();

        FinancialAuditRecord saved =
                auditRepository.findById(auditId).orElseThrow();

        assertThat(saved.getPaymentId()).isEqualTo(payment.getId());
        assertThat(saved.getAction())
                .isEqualTo(FinancialAuditAction.REFUND_REQUESTED);
        assertThat(saved.getActorType())
                .isEqualTo(FinancialAuditActorType.USER);
        assertThat(saved.getActorId()).isEqualTo("finance-user-1");
        assertThat(saved.getReason()).isEqualTo("Customer requested refund");
        assertThat(saved.getMetadata()).isEqualTo("channel=admin-api");
        assertThat(saved.getOccurredAt()).isEqualTo(FIRST_AUDIT_AT);
    }

    @Test
    void shouldReturnPaymentAuditTrailInChronologicalOrder() {

        Payment payment = persistPayment();

        FinancialAuditRecord requested =
                audit(
                        payment.getId(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        FIRST_AUDIT_AT);

        FinancialAuditRecord succeeded =
                audit(
                        payment.getId(),
                        FinancialAuditAction.REFUND_SUCCEEDED,
                        FIRST_AUDIT_AT.plusMinutes(2));

        auditRepository.saveAllAndFlush(List.of(succeeded, requested));

        entityManager.clear();

        List<FinancialAuditRecord> trail =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(
                        payment.getId());

        assertThat(trail)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactly(
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditAction.REFUND_SUCCEEDED);
    }

    @Test
    void auditForMissingPaymentShouldBeRejected() {

        FinancialAuditRecord audit =
                audit(
                        UuidGenerator.next(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        FIRST_AUDIT_AT);

        assertThatThrownBy(() -> auditRepository.saveAndFlush(audit))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void samePaymentShouldAllowMultipleAppendOnlyAuditRecords() {

        Payment payment = persistPayment();

        FinancialAuditRecord first =
                audit(
                        payment.getId(),
                        FinancialAuditAction.RECONCILIATION_OPENED,
                        FIRST_AUDIT_AT);

        FinancialAuditRecord second =
                audit(
                        payment.getId(),
                        FinancialAuditAction.RECONCILIATION_RESOLVED,
                        FIRST_AUDIT_AT.plusMinutes(5));

        auditRepository.saveAllAndFlush(List.of(first, second));

        entityManager.clear();

        assertThat(
                        auditRepository
                                .findAllByPaymentIdOrderByOccurredAtAscIdAsc(
                                        payment.getId()))
                .hasSize(2);
    }

    private Payment persistPayment() {

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("250000.00"),
                        "VND",
                        "MOCK",
                        REQUESTED_AT.plusMinutes(10),
                        REQUESTED_AT,
                        UuidGenerator.next(),
                        UuidGenerator.next());

        return paymentRepository.saveAndFlush(payment);
    }

    private static FinancialAuditRecord audit(
            UUID paymentId,
            FinancialAuditAction action,
            OffsetDateTime occurredAt) {

        return new FinancialAuditRecord(
                paymentId,
                action,
                FinancialAuditActorType.SYSTEM,
                "payment-service",
                null,
                null,
                UuidGenerator.next(),
                occurredAt);
    }
}
