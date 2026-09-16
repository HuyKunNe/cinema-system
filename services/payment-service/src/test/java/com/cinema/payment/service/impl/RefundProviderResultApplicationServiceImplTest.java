package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.AppliedProviderRefundResult;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderRefundCommand;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RefundProviderResultApplicationServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-16T07:00:00Z");

    private static final String PROCESSING_OWNER = "payment-refund-provider-operation:worker-1";

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentTransactionRepository transactionRepository;

    @Mock private FinancialAuditRecordRepository auditRepository;

    private RefundProviderResultApplicationServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.parse("2026-09-16T07:00:00Z"), ZoneOffset.UTC);

        service =
                new RefundProviderResultApplicationServiceImpl(
                        paymentRepository, transactionRepository, auditRepository, clock);
    }

    @Test
    void successfulResultShouldCompleteRefundAndTransaction() {

        Fixture fixture = fixture();

        stubLockedEntities(fixture);

        AppliedProviderRefundResult appliedResult =
                service.apply(
                        fixture.operation(), ProviderRefundResult.succeeded("provider-refund-123"));

        assertThat(appliedResult.refundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        assertThat(appliedResult.transactionStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getProviderReference()).isEqualTo("provider-charge-123");

        assertThat(fixture.transaction().getProviderReference()).isEqualTo("provider-refund-123");

        assertThat(fixture.transaction().getCompletedAt()).isEqualTo(NOW);

        assertThat(fixture.transaction().getProcessingOwner()).isNull();

        assertThat(fixture.transaction().getProcessingExpiresAt()).isNull();
        ArgumentCaptor<FinancialAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(FinancialAuditRecord.class);

        verify(auditRepository).save(auditCaptor.capture());

        FinancialAuditRecord auditRecord = auditCaptor.getValue();

        assertThat(auditRecord.getPaymentId()).isEqualTo(fixture.payment().getId());

        assertThat(auditRecord.getAction()).isEqualTo(FinancialAuditAction.REFUND_SUCCEEDED);

        assertThat(auditRecord.getActorType()).isEqualTo(FinancialAuditActorType.SYSTEM);

        assertThat(auditRecord.getActorId()).isEqualTo("payment-refund-provider-operation");

        assertThat(auditRecord.getOccurredAt()).isEqualTo(NOW);

        assertThat(auditRecord.getMetadata())
                .contains(fixture.transaction().getId().toString())
                .contains("MOCK");

        verifyLockOrder(fixture);
    }

    @Test
    void failedResultShouldFailRefundWithoutFailingCharge() {

        Fixture fixture = fixture();

        stubLockedEntities(fixture);

        service.apply(
                fixture.operation(),
                ProviderRefundResult.failed("REFUND_DECLINED", "Refund was declined"));

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.FAILED);

        assertThat(fixture.payment().getProviderReference()).isEqualTo("provider-charge-123");

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        assertThat(fixture.transaction().getFailureCode()).isEqualTo("REFUND_DECLINED");

        assertThat(fixture.transaction().getFailureMessage()).isEqualTo("Refund was declined");

        assertThat(fixture.transaction().getCompletedAt()).isEqualTo(NOW);

        ArgumentCaptor<FinancialAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(FinancialAuditRecord.class);

        verify(auditRepository).save(auditCaptor.capture());

        FinancialAuditRecord auditRecord = auditCaptor.getValue();

        assertThat(auditRecord.getAction()).isEqualTo(FinancialAuditAction.REFUND_FAILED);

        assertThat(auditRecord.getActorType()).isEqualTo(FinancialAuditActorType.SYSTEM);

        assertThat(auditRecord.getPaymentId()).isEqualTo(fixture.payment().getId());

        assertThat(auditRecord.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void pendingResultShouldRemainNonTerminal() {

        Fixture fixture = fixture();

        stubLockedEntities(fixture);

        service.apply(
                fixture.operation(), ProviderRefundResult.pending("provider-refund-pending-123"));

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(fixture.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getProviderReference())
                .isEqualTo("provider-refund-pending-123");

        assertThat(fixture.transaction().getCompletedAt()).isNull();

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    @Test
    void unknownResultShouldRemainNonTerminal() {

        Fixture fixture = fixture();

        stubLockedEntities(fixture);

        service.apply(
                fixture.operation(),
                ProviderRefundResult.unknown(
                        null, "REFUND_OUTCOME_UNKNOWN", "Refund outcome could not be determined"));

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(fixture.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getFailureCode()).isEqualTo("REFUND_OUTCOME_UNKNOWN");

        assertThat(fixture.transaction().getFailureMessage())
                .isEqualTo("Refund outcome could not be determined");

        assertThat(fixture.transaction().getCompletedAt()).isNull();

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    @Test
    void replacedLeaseOwnerShouldRejectStaleResult() {

        Fixture fixture = fixture();

        fixture.transaction()
                .claim(
                        "payment-refund-provider-operation:worker-2",
                        NOW.plusSeconds(31),
                        NOW.plusSeconds(61));

        stubLockedEntities(fixture);

        assertThatThrownBy(
                        () ->
                                service.apply(
                                        fixture.operation(),
                                        ProviderRefundResult.succeeded("provider-refund-stale")))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_TRANSACTION_LEASE_NOT_OWNED));

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    private void stubLockedEntities(Fixture fixture) {

        when(transactionRepository.findById(fixture.transaction().getId()))
                .thenReturn(Optional.of(fixture.transaction()));

        when(paymentRepository.findByIdForUpdate(fixture.payment().getId()))
                .thenReturn(Optional.of(fixture.payment()));

        when(transactionRepository.findByIdForUpdate(fixture.transaction().getId()))
                .thenReturn(Optional.of(fixture.transaction()));
    }

    private void verifyLockOrder(Fixture fixture) {

        InOrder lockOrder = inOrder(transactionRepository, paymentRepository);

        lockOrder.verify(transactionRepository).findById(fixture.transaction().getId());

        lockOrder.verify(paymentRepository).findByIdForUpdate(fixture.payment().getId());

        lockOrder.verify(transactionRepository).findByIdForUpdate(fixture.transaction().getId());
    }

    private static Fixture fixture() {

        OffsetDateTime requestedAt = NOW.minusMinutes(10);

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOCK",
                        NOW.plusMinutes(10),
                        requestedAt,
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderSuccess("provider-charge-123", NOW.minusMinutes(5));

        payment.requestRefund();

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.REFUND,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        "refund:" + payment.getId(),
                        NOW.minusMinutes(1));

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(1), NOW.plusSeconds(30));

        ProviderRefundCommand command =
                new ProviderRefundCommand(
                        payment.getId(),
                        payment.getProviderReference(),
                        payment.getAmount(),
                        payment.getCurrency());

        ClaimedProviderRefundOperation operation =
                new ClaimedProviderRefundOperation(
                        transaction.getId(),
                        PROCESSING_OWNER,
                        transaction.getProvider(),
                        transaction.getIdempotencyKey(),
                        command);

        return new Fixture(payment, transaction, operation);
    }

    private record Fixture(
            Payment payment,
            PaymentTransaction transaction,
            ClaimedProviderRefundOperation operation) {}
}
