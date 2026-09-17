package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class RefundRequestServiceMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    private static final OffsetDateTime REFUND_REQUESTED_AT =
            OffsetDateTime.parse("2026-09-16T10:05:00Z");

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private FinancialAuditRecordRepository auditRepository;

    @Autowired private EntityManager entityManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refundRequestShouldCommitPaymentTransactionAndAuditAtomically() {

        Payment payment = persistSuccessfulPayment();

        UUID correlationId = UuidGenerator.next();

        RefundRequestResult result =
                refundRequestService.requestRefund(request(payment.getId(), correlationId));

        entityManager.clear();

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId());

        assertThat(result.paymentId()).isEqualTo(payment.getId());

        assertThat(result.refundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(result.duplicate()).isFalse();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(savedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(transactions)
                .filteredOn(
                        transaction ->
                                transaction.getTransactionType() == PaymentTransactionType.REFUND)
                .singleElement()
                .satisfies(
                        transaction -> {
                            assertThat(transaction.getId()).isEqualTo(result.transactionId());

                            assertThat(transaction.getAmount())
                                    .isEqualByComparingTo(payment.getAmount());

                            assertThat(transaction.getCurrency()).isEqualTo(payment.getCurrency());

                            assertThat(transaction.getProvider()).isEqualTo(payment.getProvider());

                            assertThat(transaction.getIdempotencyKey())
                                    .isEqualTo("refund:" + payment.getId());
                        });

        assertThat(audits)
                .singleElement()
                .satisfies(
                        audit -> {
                            assertThat(audit.getAction())
                                    .isEqualTo(FinancialAuditAction.REFUND_REQUESTED);

                            assertThat(audit.getActorType())
                                    .isEqualTo(FinancialAuditActorType.USER);

                            assertThat(audit.getActorId()).isEqualTo("finance-user");

                            assertThat(audit.getCorrelationId()).isEqualTo(correlationId);
                        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void duplicateRefundRequestShouldReturnSameTransactionWithoutDuplicateAudit() {

        Payment payment = persistSuccessfulPayment();

        RefundRequest firstRequest = request(payment.getId(), UuidGenerator.next());

        RefundRequestResult first = refundRequestService.requestRefund(firstRequest);

        RefundRequestResult duplicate =
                refundRequestService.requestRefund(request(payment.getId(), UuidGenerator.next()));

        assertThat(duplicate.paymentId()).isEqualTo(first.paymentId());

        assertThat(duplicate.transactionId()).isEqualTo(first.transactionId());

        assertThat(duplicate.refundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(duplicate.duplicate()).isTrue();

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertThat(transactions)
                .filteredOn(
                        transaction ->
                                transaction.getTransactionType() == PaymentTransactionType.REFUND)
                .hasSize(1);

        assertThat(auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId()))
                .extracting(FinancialAuditRecord::getAction)
                .containsExactly(FinancialAuditAction.REFUND_REQUESTED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRefundRequestsShouldCreateExactlyOneRefundTransactionAndAudit()
            throws Exception {

        Payment payment = persistSuccessfulPayment();

        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<RefundRequestResult> first =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return refundRequestService.requestRefund(
                                        request(payment.getId(), UuidGenerator.next()));
                            });

            Future<RefundRequestResult> second =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return refundRequestService.requestRefund(
                                        request(payment.getId(), UuidGenerator.next()));
                            });

            startGate.countDown();

            RefundRequestResult firstResult = first.get(10, TimeUnit.SECONDS);

            RefundRequestResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.paymentId()).isEqualTo(payment.getId());

            assertThat(secondResult.paymentId()).isEqualTo(payment.getId());

            assertThat(firstResult.transactionId()).isEqualTo(secondResult.transactionId());

            assertThat(
                            List.of(firstResult, secondResult).stream()
                                    .filter(result -> !result.duplicate())
                                    .count())
                    .isEqualTo(1);

            assertThat(
                            List.of(firstResult, secondResult).stream()
                                    .filter(RefundRequestResult::duplicate)
                                    .count())
                    .isEqualTo(1);
        }

        entityManager.clear();

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        List<PaymentTransaction> refundTransactions =
                transactionRepository
                        .findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId())
                        .stream()
                        .filter(
                                transaction ->
                                        transaction.getTransactionType()
                                                == PaymentTransactionType.REFUND)
                        .toList();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId());

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(savedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(refundTransactions).hasSize(1);

        assertThat(refundTransactions.getFirst().getIdempotencyKey())
                .isEqualTo("refund:" + payment.getId());

        assertThat(audits)
                .filteredOn(audit -> audit.getAction() == FinancialAuditAction.REFUND_REQUESTED)
                .hasSize(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void nonSuccessfulPaymentShouldRejectRefundWithoutSideEffects() {

        Payment payment = persistReceivedPayment();

        assertThatThrownBy(
                        () ->
                                refundRequestService.requestRefund(
                                        request(payment.getId(), UuidGenerator.next())))
                .isInstanceOf(ConflictException.class);

        Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.RECEIVED);

        assertThat(saved.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUESTED);

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .isEmpty();

        assertThat(auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId()))
                .isEmpty();
    }

    private Payment persistSuccessfulPayment() {

        Payment payment = newPayment();

        payment.startProcessing();

        payment.completeProviderSuccess(
                "charge-" + UuidGenerator.next(), REQUESTED_AT.plusMinutes(1));

        return paymentRepository.saveAndFlush(payment);
    }

    private Payment persistReceivedPayment() {

        return paymentRepository.saveAndFlush(newPayment());
    }

    private static Payment newPayment() {

        return new Payment(
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
    }

    private static RefundRequest request(UUID paymentId, UUID correlationId) {

        return new RefundRequest(
                paymentId,
                FinancialAuditActorType.USER,
                "finance-user",
                "Customer requested refund",
                correlationId,
                REFUND_REQUESTED_AT);
    }
}
