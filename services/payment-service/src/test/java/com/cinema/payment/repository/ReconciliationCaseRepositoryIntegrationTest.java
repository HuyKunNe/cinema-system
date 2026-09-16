package com.cinema.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;

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
class ReconciliationCaseRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT =
            OffsetDateTime.parse("2026-09-16T09:00:00Z");

    private static final OffsetDateTime OPENED_AT =
            OffsetDateTime.parse("2026-09-16T10:00:00Z");

    private static final OffsetDateTime RESOLVED_AT =
            OffsetDateTime.parse("2026-09-16T11:00:00Z");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private ReconciliationCaseRepository reconciliationCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistOpenReconciliationCase() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        payment.getProvider(),
                        "refund-provider-reference",
                        OPENED_AT);

        reconciliationCaseRepository.saveAndFlush(reconciliationCase);

        UUID caseId = reconciliationCase.getId();

        entityManager.clear();

        ReconciliationCase saved =
                reconciliationCaseRepository
                        .findById(caseId)
                        .orElseThrow();

        assertThat(saved.getPaymentId())
                .isEqualTo(payment.getId());

        assertThat(saved.getPaymentTransactionId())
                .isEqualTo(transaction.getId());

        assertThat(saved.getStatus())
                .isEqualTo(ReconciliationStatus.OPEN);

        assertThat(saved.getReason())
                .isEqualTo(ReconciliationReason.REFUND_PROVIDER_UNKNOWN);

        assertThat(saved.getResolution())
                .isNull();

        assertThat(saved.getProvider())
                .isEqualTo("MOCK");

        assertThat(saved.getProviderReference())
                .isEqualTo("refund-provider-reference");

        assertThat(saved.getOpenedAt())
                .isEqualTo(OPENED_AT);

        assertThat(saved.getResolvedAt())
                .isNull();

        assertThat(saved.getResolvedByType())
                .isNull();

        assertThat(saved.getResolvedBy())
                .isNull();
    }

    @Test
    void shouldPersistResolvedReconciliationCase() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase reconciliationCase =
                reconciliationCase(payment, transaction);

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "finance-admin",
                "Provider dashboard confirms refund",
                RESOLVED_AT);

        reconciliationCaseRepository.saveAndFlush(reconciliationCase);

        UUID caseId = reconciliationCase.getId();

        entityManager.clear();

        ReconciliationCase saved =
                reconciliationCaseRepository
                        .findById(caseId)
                        .orElseThrow();

        assertThat(saved.getStatus())
                .isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(saved.getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        assertThat(saved.getResolvedAt())
                .isEqualTo(RESOLVED_AT);

        assertThat(saved.getResolvedByType())
                .isEqualTo(FinancialAuditActorType.USER);

        assertThat(saved.getResolvedBy())
                .isEqualTo("finance-admin");

        assertThat(saved.getResolutionReason())
                .isEqualTo("Provider dashboard confirms refund");
    }

    @Test
    void shouldPersistRejectedReconciliationCase() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase reconciliationCase =
                reconciliationCase(payment, transaction);

        reconciliationCase.reject(
                FinancialAuditActorType.USER,
                "finance-admin",
                "Provider observation was invalid",
                RESOLVED_AT);

        reconciliationCaseRepository.saveAndFlush(reconciliationCase);

        UUID caseId = reconciliationCase.getId();

        entityManager.clear();

        ReconciliationCase saved =
                reconciliationCaseRepository
                        .findById(caseId)
                        .orElseThrow();

        assertThat(saved.getStatus())
                .isEqualTo(ReconciliationStatus.REJECTED);

        assertThat(saved.getResolution())
                .isNull();

        assertThat(saved.getResolvedAt())
                .isEqualTo(RESOLVED_AT);

        assertThat(saved.getResolvedByType())
                .isEqualTo(FinancialAuditActorType.USER);

        assertThat(saved.getResolvedBy())
                .isEqualTo("finance-admin");
    }

    @Test
    void shouldFindCaseByPaymentTransactionId() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase reconciliationCase =
                reconciliationCase(payment, transaction);

        reconciliationCaseRepository.saveAndFlush(reconciliationCase);

        entityManager.clear();

        ReconciliationCase found =
                reconciliationCaseRepository
                        .findByPaymentTransactionId(transaction.getId())
                        .orElseThrow();

        assertThat(found.getId())
                .isEqualTo(reconciliationCase.getId());
    }

    @Test
    void shouldReturnPaymentCasesInOpenedOrder() {

        Payment payment = persistPayment();

        PaymentTransaction firstTransaction =
                persistRefundTransaction(
                        payment,
                        "refund:" + payment.getId() + ":1",
                        1);

        PaymentTransaction secondTransaction =
                persistRefundTransaction(
                        payment,
                        "refund:" + payment.getId() + ":2",
                        2);

        ReconciliationCase first =
                new ReconciliationCase(
                        payment.getId(),
                        firstTransaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_PENDING,
                        payment.getProvider(),
                        null,
                        OPENED_AT);

        ReconciliationCase second =
                new ReconciliationCase(
                        payment.getId(),
                        secondTransaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        payment.getProvider(),
                        null,
                        OPENED_AT.plusMinutes(5));

        reconciliationCaseRepository.saveAllAndFlush(
                List.of(second, first));

        entityManager.clear();

        List<ReconciliationCase> cases =
                reconciliationCaseRepository
                        .findAllByPaymentIdOrderByOpenedAtAscIdAsc(
                                payment.getId());

        assertThat(cases)
                .extracting(ReconciliationCase::getId)
                .containsExactly(
                        first.getId(),
                        second.getId());
    }

    @Test
    void sameTransactionShouldNotAllowMultipleReconciliationCases() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase first =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_PENDING,
                        payment.getProvider(),
                        null,
                        OPENED_AT);

        reconciliationCaseRepository.saveAndFlush(first);

        ReconciliationCase duplicate =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        payment.getProvider(),
                        null,
                        OPENED_AT.plusMinutes(1));

        assertThatThrownBy(
                        () ->
                                reconciliationCaseRepository
                                        .saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void missingPaymentShouldBeRejectedByForeignKey() {

        Payment payment = persistPayment();
        PaymentTransaction transaction = persistRefundTransaction(payment);

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        UuidGenerator.next(),
                        transaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        payment.getProvider(),
                        null,
                        OPENED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCaseRepository
                                        .saveAndFlush(reconciliationCase))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void missingTransactionShouldBeRejectedByForeignKey() {

        Payment payment = persistPayment();

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        payment.getId(),
                        UuidGenerator.next(),
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        payment.getProvider(),
                        null,
                        OPENED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCaseRepository
                                        .saveAndFlush(reconciliationCase))
                .isInstanceOf(DataIntegrityViolationException.class);
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

    private PaymentTransaction persistRefundTransaction(
            Payment payment) {

        return persistRefundTransaction(
                payment,
                "refund:" + payment.getId(),
                1);
    }

    private PaymentTransaction persistRefundTransaction(
            Payment payment,
            String idempotencyKey,
            int attemptNumber) {

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.REFUND,
                        attemptNumber,
                        payment.getAmount(),
                        payment.getCurrency(),
                        idempotencyKey,
                        OPENED_AT.minusMinutes(1));

        return transactionRepository.saveAndFlush(transaction);
    }

    private static ReconciliationCase reconciliationCase(
            Payment payment,
            PaymentTransaction transaction) {

        return new ReconciliationCase(
                payment.getId(),
                transaction.getId(),
                ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                payment.getProvider(),
                "refund-provider-reference",
                OPENED_AT);
    }
}
