package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class FinancialAuditReadServiceMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime PAYMENT_REQUESTED_AT =
            OffsetDateTime.parse("2026-09-17T05:00:00Z");

    private static final OffsetDateTime EARLY = OffsetDateTime.parse("2026-09-17T05:01:00Z");

    private static final OffsetDateTime SAME_TIME = OffsetDateTime.parse("2026-09-17T05:02:00Z");

    private static final OffsetDateTime LATE = OffsetDateTime.parse("2026-09-17T05:03:00Z");

    @Autowired private FinancialAuditReadService financialAuditReadService;

    @Autowired private FinancialAuditRecordRepository financialAuditRecordRepository;

    @Autowired private PaymentRepository paymentRepository;

    @Test
    @Transactional
    void auditReadShouldReturnOnlyRequestedPaymentInStableChronologicalOrder() {

        Payment targetPayment = paymentRepository.saveAndFlush(newPayment());

        Payment unrelatedPayment = paymentRepository.saveAndFlush(newPayment());

        /*
         * UUIDs are generated before persistence.
         *
         * Two records intentionally share the same occurredAt value so that
         * the repository's secondary ORDER BY id ASC is exercised.
         */
        FinancialAuditRecord sameTimeFirst =
                audit(
                        targetPayment.getId(),
                        FinancialAuditAction.RECONCILIATION_OPENED,
                        "system-reconciliation",
                        "Reconciliation opened",
                        "provider=MOCK",
                        SAME_TIME);

        FinancialAuditRecord late =
                audit(
                        targetPayment.getId(),
                        FinancialAuditAction.RECONCILIATION_RESOLVED,
                        "finance-admin",
                        "Provider evidence confirmed",
                        "provider=MOCK",
                        LATE);

        FinancialAuditRecord early =
                audit(
                        targetPayment.getId(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        "finance-user",
                        "Customer requested refund",
                        null,
                        EARLY);

        FinancialAuditRecord sameTimeSecond =
                audit(
                        targetPayment.getId(),
                        FinancialAuditAction.RECONCILIATION_REJECTED,
                        "finance-admin-2",
                        "Insufficient provider evidence",
                        "provider=MOCK",
                        SAME_TIME);

        FinancialAuditRecord unrelated =
                audit(
                        unrelatedPayment.getId(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        "other-user",
                        "Other payment refund",
                        null,
                        PAYMENT_REQUESTED_AT);

        /*
         * Persist intentionally out of chronological order.
         */
        financialAuditRecordRepository.saveAllAndFlush(
                List.of(late, sameTimeSecond, unrelated, early, sameTimeFirst));

        List<FinancialAuditRecord> persistedOrder =
                financialAuditRecordRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(
                        targetPayment.getId());

        List<FinancialAuditRecordResponse> result =
                financialAuditReadService.findByPaymentId(targetPayment.getId());

        /*
         * Payment isolation:
         * audit rows belonging to another Payment must never appear.
         */
        assertThat(result).hasSize(4);

        assertThat(result)
                .allSatisfy(
                        record -> assertThat(record.paymentId()).isEqualTo(targetPayment.getId()));

        assertThat(result)
                .extracting(FinancialAuditRecordResponse::paymentId)
                .doesNotContain(unrelatedPayment.getId());

        /*
         * Primary ordering contract:
         * occurredAt ASC.
         */
        assertThat(result)
                .extracting(FinancialAuditRecordResponse::occurredAt)
                .containsExactly(EARLY, SAME_TIME, SAME_TIME, LATE);

        /*
         * Verify service preserves the exact stable database order,
         * including the id ASC tie-breaker for equal occurredAt values.
         */
        assertThat(result)
                .extracting(FinancialAuditRecordResponse::id)
                .containsExactlyElementsOf(
                        persistedOrder.stream().map(FinancialAuditRecord::getId).toList());

        List<UUID> persistedSameTimeIds =
                persistedOrder.stream()
                        .filter(record -> record.getOccurredAt().equals(SAME_TIME))
                        .map(FinancialAuditRecord::getId)
                        .toList();

        List<UUID> responseSameTimeIds =
                result.stream()
                        .filter(record -> record.occurredAt().equals(SAME_TIME))
                        .map(FinancialAuditRecordResponse::id)
                        .toList();

        assertThat(responseSameTimeIds).containsExactlyElementsOf(persistedSameTimeIds);

        /*
         * Mapping verification:
         * persisted audit fields must survive the read boundary.
         */
        FinancialAuditRecordResponse first = result.getFirst();

        assertThat(first.id()).isEqualTo(early.getId());

        assertThat(first.paymentId()).isEqualTo(targetPayment.getId());

        assertThat(first.action()).isEqualTo(FinancialAuditAction.REFUND_REQUESTED);

        assertThat(first.actorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(first.actorId()).isEqualTo("finance-user");

        assertThat(first.reason()).isEqualTo("Customer requested refund");

        assertThat(first.metadata()).isNull();

        assertThat(first.correlationId()).isEqualTo(early.getCorrelationId());

        assertThat(first.occurredAt()).isEqualTo(EARLY);
    }

    @Test
    @Transactional
    void auditReadShouldReturnEmptyListForExistingPaymentWithoutAuditRecords() {

        Payment payment = paymentRepository.saveAndFlush(newPayment());

        List<FinancialAuditRecordResponse> result =
                financialAuditReadService.findByPaymentId(payment.getId());

        assertThat(result).isEmpty();
    }

    private static Payment newPayment() {

        return new Payment(
                UuidGenerator.next(),
                UuidGenerator.next(),
                1,
                new BigDecimal("125000.00"),
                "VND",
                "MOCK",
                PAYMENT_REQUESTED_AT.plusMinutes(10),
                PAYMENT_REQUESTED_AT,
                UuidGenerator.next(),
                UuidGenerator.next());
    }

    private static FinancialAuditRecord audit(
            UUID paymentId,
            FinancialAuditAction action,
            String actorId,
            String reason,
            String metadata,
            OffsetDateTime occurredAt) {

        return new FinancialAuditRecord(
                paymentId,
                action,
                FinancialAuditActorType.USER,
                actorId,
                reason,
                metadata,
                UuidGenerator.next(),
                occurredAt);
    }
}
