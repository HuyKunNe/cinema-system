package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

class RefundPaymentProviderOperationWorkerMySqlIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private FinancialAuditRecordRepository auditRepository;

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private RefundPaymentProviderOperationWorker refundWorker;

    @Test
    void workerShouldClaimExecuteAndApplyReadyRefund() {

        Payment payment = successfulPayment();

        paymentRepository.saveAndFlush(payment);

        refundRequestService.requestRefund(
                new RefundRequest(
                        payment.getId(),
                        FinancialAuditActorType.USER,
                        "finance-user",
                        "Customer requested refund",
                        UuidGenerator.next(),
                        REQUESTED_AT.plusMinutes(2)));

        PaymentTransaction beforeProcessing = findRefundTransaction(payment.getId());

        assertThat(beforeProcessing.getStatus()).isEqualTo(PaymentTransactionStatus.READY);

        assertThat(beforeProcessing.getProcessingOwner()).isNull();

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getRefundStatus())
                .isEqualTo(RefundStatus.PENDING);

        PaymentProviderOperationBatchResult batchResult = refundWorker.processNextBatch();

        assertThat(batchResult.claimedCount()).isEqualTo(1);

        assertThat(batchResult.appliedCount()).isEqualTo(1);

        assertThat(batchResult.failedCount()).isZero();

        Payment persistedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        PaymentTransaction persistedRefund =
                transactionRepository.findById(beforeProcessing.getId()).orElseThrow();

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        /*
         * Refund execution must never replace the original
         * charge provider reference stored on Payment.
         */
        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(payment.getProviderReference());

        assertThat(persistedRefund.getTransactionType()).isEqualTo(PaymentTransactionType.REFUND);

        assertThat(persistedRefund.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(persistedRefund.getProviderReference()).isNotBlank();

        assertThat(persistedRefund.getProcessingOwner()).isNull();

        assertThat(persistedRefund.getProcessingExpiresAt()).isNull();

        assertThat(persistedRefund.getCompletedAt()).isNotNull();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId());

        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactlyInAnyOrder(
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditAction.REFUND_SUCCEEDED);
    }

    private PaymentTransaction findRefundTransaction(java.util.UUID paymentId) {

        return transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(paymentId).stream()
                .filter(
                        transaction ->
                                transaction.getTransactionType() == PaymentTransactionType.REFUND)
                .findFirst()
                .orElseThrow();
    }

    private static Payment successfulPayment() {

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

        payment.startProcessing();

        payment.completeProviderSuccess(
                "mock-charge-reference-" + UuidGenerator.next(), REQUESTED_AT.plusMinutes(1));

        return payment;
    }
}
