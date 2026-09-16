package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class PaymentProviderRefundPreparationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private PaymentTransactionClaimService claimService;

    @Autowired private PaymentProviderOperationPreparationService preparationService;

    @Test
    void shouldPrepareClaimedRefundUsingOriginalChargeReference() {

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

        /*
         * R27.10.4 intentionally prevents the generic charge claim service
         * from claiming REFUND operations, so claim this transaction through
         * the domain object to exercise preparation.
         */
        OffsetDateTime claimedAt = OffsetDateTime.now();

        String owner = "refund-test-owner";

        refundTransaction.claim(owner, claimedAt, claimedAt.plusMinutes(1));

        transactionRepository.saveAndFlush(refundTransaction);

        ClaimedProviderRefundOperation operation =
                preparationService.prepareRefund(refundTransaction.getId(), owner);

        assertThat(operation.transactionId()).isEqualTo(refundTransaction.getId());

        assertThat(operation.provider()).isEqualTo("MOCK");

        assertThat(operation.idempotencyKey()).isEqualTo("refund:" + payment.getId());

        assertThat(operation.command().paymentId()).isEqualTo(payment.getId());

        assertThat(operation.command().chargeProviderReference())
                .isEqualTo(payment.getProviderReference());

        assertThat(operation.command().amount()).isEqualByComparingTo(payment.getAmount());

        assertThat(operation.command().currency()).isEqualTo(payment.getCurrency());
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

        payment.completeProviderSuccess("mock-charge-reference", REQUESTED_AT.plusMinutes(1));

        return payment;
    }
}
