package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ReconciliationCaseRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

class RefundProviderResultApplicationMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private FinancialAuditRecordRepository auditRepository;

    @Autowired private RefundRequestService refundRequestService;

    @Autowired private PaymentProviderOperationPreparationService preparationService;

    @Autowired private RefundProviderResultApplicationService resultApplicationService;

    @Autowired private ReconciliationCaseRepository reconciliationCaseRepository;

    @Test
    void successfulProviderResultShouldAtomicallyCompleteRefundTransactionAndAudit() {

        Fixture fixture = claimedRefundFixture();

        resultApplicationService.apply(
                fixture.operation(), ProviderRefundResult.succeeded("provider-refund-123"));

        Payment persistedPayment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        /*
         * Original charge reference belongs to Payment and must not be
         * replaced by the refund provider reference.
         */
        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(fixture.chargeProviderReference());

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(persistedTransaction.getProviderReference()).isEqualTo("provider-refund-123");

        assertThat(persistedTransaction.getProcessingOwner()).isNull();

        assertThat(persistedTransaction.getProcessingExpiresAt()).isNull();

        assertThat(reconciliationCaseRepository.findByPaymentTransactionId(fixture.transactionId()))
                .isEmpty();

        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactlyInAnyOrder(
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditAction.REFUND_SUCCEEDED);
    }

    @Test
    void failedProviderResultShouldAtomicallyFailRefundWithoutChangingSuccessfulCharge() {

        Fixture fixture = claimedRefundFixture();

        resultApplicationService.apply(
                fixture.operation(),
                ProviderRefundResult.failed("REFUND_DECLINED", "Refund was declined"));

        Payment persistedPayment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.FAILED);

        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(fixture.chargeProviderReference());

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        assertThat(persistedTransaction.getFailureCode()).isEqualTo("REFUND_DECLINED");

        assertThat(persistedTransaction.getFailureMessage()).isEqualTo("Refund was declined");

        assertThat(persistedTransaction.getProcessingOwner()).isNull();

        assertThat(persistedTransaction.getProcessingExpiresAt()).isNull();

        assertThat(reconciliationCaseRepository.findByPaymentTransactionId(fixture.transactionId()))
                .isEmpty();

        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactlyInAnyOrder(
                        FinancialAuditAction.REFUND_REQUESTED, FinancialAuditAction.REFUND_FAILED);
    }

    @Test
    void pendingProviderResultShouldOpenReconciliationCaseAndAudit() {

        Fixture fixture = claimedRefundFixture();

        resultApplicationService.apply(
                fixture.operation(), ProviderRefundResult.pending("provider-refund-pending-123"));

        Payment persistedPayment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository
                        .findByPaymentTransactionId(fixture.transactionId())
                        .orElseThrow();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(fixture.chargeProviderReference());

        assertThat(persistedTransaction.getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(persistedTransaction.getProviderReference())
                .isEqualTo("provider-refund-pending-123");

        assertThat(persistedTransaction.getProcessingOwner()).isNull();

        assertThat(persistedTransaction.getProcessingExpiresAt()).isNull();

        assertThat(reconciliationCase.getPaymentId()).isEqualTo(fixture.paymentId());

        assertThat(reconciliationCase.getPaymentTransactionId()).isEqualTo(fixture.transactionId());

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        assertThat(reconciliationCase.getReason())
                .isEqualTo(ReconciliationReason.REFUND_PROVIDER_PENDING);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getProvider()).isEqualTo("MOCK");

        assertThat(reconciliationCase.getProviderReference())
                .isEqualTo("provider-refund-pending-123");

        assertThat(reconciliationCase.getResolvedAt()).isNull();

        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactlyInAnyOrder(
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditAction.RECONCILIATION_OPENED);
    }

    @Test
    void unknownProviderResultShouldOpenReconciliationCaseAndAudit() {

        Fixture fixture = claimedRefundFixture();

        resultApplicationService.apply(
                fixture.operation(),
                ProviderRefundResult.unknown(
                        "provider-refund-unknown-123",
                        "PROVIDER_TIMEOUT",
                        "Provider outcome could not be determined"));

        Payment persistedPayment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository
                        .findByPaymentTransactionId(fixture.transactionId())
                        .orElseThrow();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(persistedPayment.getProviderReference())
                .isEqualTo(fixture.chargeProviderReference());

        assertThat(persistedTransaction.getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(persistedTransaction.getProviderReference())
                .isEqualTo("provider-refund-unknown-123");

        assertThat(persistedTransaction.getFailureCode()).isEqualTo("PROVIDER_TIMEOUT");

        assertThat(persistedTransaction.getFailureMessage())
                .isEqualTo("Provider outcome could not be determined");

        assertThat(persistedTransaction.getProcessingOwner()).isNull();

        assertThat(persistedTransaction.getProcessingExpiresAt()).isNull();

        assertThat(reconciliationCase.getPaymentId()).isEqualTo(fixture.paymentId());

        assertThat(reconciliationCase.getPaymentTransactionId()).isEqualTo(fixture.transactionId());

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        assertThat(reconciliationCase.getReason())
                .isEqualTo(ReconciliationReason.REFUND_PROVIDER_UNKNOWN);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getProvider()).isEqualTo("MOCK");

        assertThat(reconciliationCase.getProviderReference())
                .isEqualTo("provider-refund-unknown-123");

        assertThat(audits)
                .extracting(FinancialAuditRecord::getAction)
                .containsExactlyInAnyOrder(
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditAction.RECONCILIATION_OPENED);

        assertThat(audits)
                .extracting(FinancialAuditRecord::getMetadata)
                .allSatisfy(
                        metadata -> {
                            if (metadata != null) {
                                assertThat(metadata)
                                        .doesNotContain("Provider outcome could not be determined");
                            }
                        });
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

        String owner = "refund-result-test-owner";

        OffsetDateTime claimedAt = OffsetDateTime.now();

        refundTransaction.claim(owner, claimedAt, claimedAt.plusMinutes(1));

        transactionRepository.saveAndFlush(refundTransaction);

        ClaimedProviderRefundOperation operation =
                preparationService.prepareRefund(refundTransaction.getId(), owner);

        return new Fixture(
                payment.getId(),
                refundTransaction.getId(),
                payment.getProviderReference(),
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
            ClaimedProviderRefundOperation operation) {}
}
