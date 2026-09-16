package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class RefundRequestRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @MockitoSpyBean private FinancialAuditRecordRepository auditRepository;

    @Test
    void auditPersistenceFailureShouldRollbackRefundRequestTransaction() {

        Payment payment = successfulPayment();

        paymentRepository.saveAndFlush(payment);

        doThrow(new RuntimeException("Forced financial audit failure"))
                .when(auditRepository)
                .save(any(FinancialAuditRecord.class));

        RefundRequest request =
                new RefundRequest(
                        payment.getId(),
                        FinancialAuditActorType.USER,
                        "finance-user",
                        "Customer requested refund",
                        UuidGenerator.next(),
                        REQUESTED_AT.plusMinutes(2));

        assertThatThrownBy(() -> refundRequestService.requestRefund(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forced financial audit failure");

        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persisted.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUESTED);

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .noneMatch(
                        transaction ->
                                transaction.getTransactionType() == PaymentTransactionType.REFUND);

        assertThat(auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(payment.getId()))
                .isEmpty();
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
                "charge-" + UuidGenerator.next(), REQUESTED_AT.plusMinutes(1));

        return payment;
    }
}
