package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

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
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

class RefundProviderResultApplicationRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @MockitoSpyBean private FinancialAuditRecordRepository auditRepository;

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private PaymentProviderOperationPreparationService preparationService;

    @Autowired private RefundProviderResultApplicationService resultApplicationService;

    @Test
    void auditFailureShouldRollbackSuccessfulRefundResultApplication() {

        Fixture fixture = claimedRefundFixture();

        doThrow(new RuntimeException("Forced refund result audit failure"))
                .when(auditRepository)
                .save(any(FinancialAuditRecord.class));

        assertThatThrownBy(
                        () ->
                                resultApplicationService.apply(
                                        fixture.operation(),
                                        ProviderRefundResult.succeeded("provider-refund-rollback")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forced refund result audit failure");

        assertRolledBack(fixture);
    }

    @Test
    void auditFailureShouldRollbackFailedRefundResultApplication() {

        Fixture fixture = claimedRefundFixture();

        doThrow(new RuntimeException("Forced refund result audit failure"))
                .when(auditRepository)
                .save(any(FinancialAuditRecord.class));

        assertThatThrownBy(
                        () ->
                                resultApplicationService.apply(
                                        fixture.operation(),
                                        ProviderRefundResult.failed(
                                                "REFUND_DECLINED", "Refund was declined")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forced refund result audit failure");

        assertRolledBack(fixture);
    }

    private void assertRolledBack(Fixture fixture) {

        Payment persistedPayment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        /*
         * Original charge remains successful and refund remains pending.
         */
        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(fixture.chargeProviderReference());

        /*
         * The REFUND transaction must return to exactly the persisted state
         * from before result application.
         */
        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(persistedTransaction.getProviderReference()).isNull();

        assertThat(persistedTransaction.getFailureCode()).isNull();

        assertThat(persistedTransaction.getFailureMessage()).isNull();

        assertThat(persistedTransaction.getCompletedAt()).isNull();

        assertThat(persistedTransaction.getProcessingOwner()).isEqualTo(fixture.processingOwner());

        assertThat(persistedTransaction.getProcessingExpiresAt()).isNotNull();

        /*
         * REFUND_REQUESTED existed before result application and must remain.
         * Terminal result audit must have rolled back.
         */
        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactly(FinancialAuditAction.REFUND_REQUESTED);
    }

    private Fixture claimedRefundFixture() {

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

        PaymentTransaction refundTransaction =
                transactionRepository
                        .findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId())
                        .stream()
                        .filter(
                                transaction ->
                                        transaction.getTransactionType()
                                                == PaymentTransactionType.REFUND)
                        .findFirst()
                        .orElseThrow();

        String owner = "refund-result-rollback-owner";

        OffsetDateTime claimedAt = OffsetDateTime.now();

        refundTransaction.claim(owner, claimedAt, claimedAt.plusMinutes(1));

        transactionRepository.saveAndFlush(refundTransaction);

        ClaimedProviderRefundOperation operation =
                preparationService.prepareRefund(refundTransaction.getId(), owner);

        return new Fixture(
                payment.getId(),
                refundTransaction.getId(),
                payment.getProviderReference(),
                owner,
                operation);
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

    private record Fixture(
            java.util.UUID paymentId,
            java.util.UUID transactionId,
            String chargeProviderReference,
            String processingOwner,
            ClaimedProviderRefundOperation operation) {}
}
