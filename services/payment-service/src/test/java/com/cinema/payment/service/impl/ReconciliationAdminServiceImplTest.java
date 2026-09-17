package com.cinema.payment.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
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
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.model.ReconciliationCaseLockReference;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ReconciliationCaseRepository;

@ExtendWith(MockitoExtension.class)
class ReconciliationAdminServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T03:00:00Z");

    private static final String PROCESSING_OWNER = "payment-refund-provider-operation:worker-1";

    private static final String ACTOR_ID = "admin-user-123";

    @Mock private ReconciliationCaseRepository reconciliationCaseRepository;

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentTransactionRepository transactionRepository;

    @Mock private FinancialAuditRecordRepository auditRepository;

    private ReconciliationAdminServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.parse("2026-09-17T03:00:00Z"), ZoneOffset.UTC);

        service =
                new ReconciliationAdminServiceImpl(
                        reconciliationCaseRepository,
                        paymentRepository,
                        transactionRepository,
                        auditRepository,
                        clock);
    }

    @Test
    void resolveRefundSucceededShouldCompleteRefundTransactionCaseAndAudit() {

        Fixture fixture = fixture("provider-refund-pending-123", null, null);

        stubLockedEntities(fixture);

        UUID correlationId = UuidGenerator.next();

        ReconciliationResolveRequest request =
                new ReconciliationResolveRequest(
                        fixture.reconciliationCase().getId(),
                        ReconciliationResolution.REFUND_SUCCEEDED,
                        FinancialAuditActorType.USER,
                        ACTOR_ID,
                        "Provider dashboard confirmed refund",
                        "provider-refund-final-123",
                        null,
                        null,
                        correlationId,
                        NOW.minusSeconds(5));

        ReconciliationOperationResult result = service.resolve(request);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(fixture.transaction().getProviderReference())
                .isEqualTo("provider-refund-final-123");

        assertThat(fixture.transaction().getFailureCode()).isNull();
        assertThat(fixture.transaction().getFailureMessage()).isNull();
        assertThat(fixture.transaction().getCompletedAt()).isEqualTo(NOW);

        assertThat(fixture.reconciliationCase().getStatus())
                .isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(fixture.reconciliationCase().getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        assertThat(fixture.reconciliationCase().getResolvedAt()).isEqualTo(NOW);

        assertThat(fixture.reconciliationCase().getResolvedByType())
                .isEqualTo(FinancialAuditActorType.USER);

        assertThat(fixture.reconciliationCase().getResolvedBy()).isEqualTo(ACTOR_ID);

        assertThat(result.refundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        assertThat(result.transactionStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(result.reconciliationStatus()).isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(result.resolution()).isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        ArgumentCaptor<FinancialAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(FinancialAuditRecord.class);

        verify(auditRepository).save(auditCaptor.capture());

        FinancialAuditRecord audit = auditCaptor.getValue();

        assertThat(audit.getPaymentId()).isEqualTo(fixture.payment().getId());

        assertThat(audit.getAction()).isEqualTo(FinancialAuditAction.RECONCILIATION_RESOLVED);

        assertThat(audit.getActorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(audit.getActorId()).isEqualTo(ACTOR_ID);

        assertThat(audit.getReason()).isEqualTo("Provider dashboard confirmed refund");

        assertThat(audit.getCorrelationId()).isEqualTo(correlationId);

        assertThat(audit.getOccurredAt()).isEqualTo(NOW);

        assertThat(audit.getMetadata())
                .contains(fixture.reconciliationCase().getId().toString())
                .contains(fixture.transaction().getId().toString())
                .contains("MOCK")
                .doesNotContain("provider-refund-final-123");

        verifyLockOrder(fixture);
    }

    @Test
    void resolveRefundSucceededShouldReuseExistingProviderReferenceWhenRequestOmitsIt() {

        Fixture fixture = fixture("provider-refund-existing-123", null, null);

        stubLockedEntities(fixture);

        ReconciliationResolveRequest request =
                resolveRequest(
                        fixture, ReconciliationResolution.REFUND_SUCCEEDED, null, null, null);

        service.resolve(request);

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(fixture.transaction().getProviderReference())
                .isEqualTo("provider-refund-existing-123");

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        assertThat(fixture.reconciliationCase().getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);
    }

    @Test
    void resolveRefundFailedShouldFailRefundTransactionWithoutChangingOriginalCharge() {

        Fixture fixture =
                fixture(
                        "provider-refund-unknown-123",
                        "PROVIDER_TIMEOUT",
                        "Provider outcome could not be determined");

        stubLockedEntities(fixture);

        UUID correlationId = UuidGenerator.next();

        ReconciliationResolveRequest request =
                new ReconciliationResolveRequest(
                        fixture.reconciliationCase().getId(),
                        ReconciliationResolution.REFUND_FAILED,
                        FinancialAuditActorType.USER,
                        ACTOR_ID,
                        "Provider dashboard confirmed refund failure",
                        null,
                        "REFUND_DECLINED",
                        "Refund was declined",
                        correlationId,
                        NOW.minusSeconds(5));

        ReconciliationOperationResult result = service.resolve(request);

        /*
         * Original charge remains successful.
         */
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getProviderReference()).isEqualTo("provider-charge-123");

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.FAILED);

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        assertThat(fixture.transaction().getFailureCode()).isEqualTo("REFUND_DECLINED");

        assertThat(fixture.transaction().getFailureMessage()).isEqualTo("Refund was declined");

        assertThat(fixture.transaction().getCompletedAt()).isEqualTo(NOW);

        assertThat(fixture.reconciliationCase().getStatus())
                .isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(fixture.reconciliationCase().getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_FAILED);

        assertThat(result.refundStatus()).isEqualTo(RefundStatus.FAILED);

        assertThat(result.transactionStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        ArgumentCaptor<FinancialAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(FinancialAuditRecord.class);

        verify(auditRepository).save(auditCaptor.capture());

        FinancialAuditRecord audit = auditCaptor.getValue();

        assertThat(audit.getAction()).isEqualTo(FinancialAuditAction.RECONCILIATION_RESOLVED);

        assertThat(audit.getCorrelationId()).isEqualTo(correlationId);

        /*
         * Provider failure details must not leak into audit metadata.
         */
        assertThat(audit.getMetadata())
                .contains(fixture.reconciliationCase().getId().toString())
                .contains(fixture.transaction().getId().toString())
                .contains("MOCK")
                .doesNotContain("REFUND_DECLINED")
                .doesNotContain("Refund was declined")
                .doesNotContain("PROVIDER_TIMEOUT")
                .doesNotContain("Provider outcome could not be determined");

        verifyLockOrder(fixture);
    }

    @Test
    void rejectShouldCloseCaseWithoutChangingRefundFinancialState() {

        Fixture fixture = fixture("provider-refund-pending-123", null, null);

        stubLockedEntities(fixture);

        UUID correlationId = UuidGenerator.next();

        ReconciliationRejectRequest request =
                new ReconciliationRejectRequest(
                        fixture.reconciliationCase().getId(),
                        FinancialAuditActorType.USER,
                        ACTOR_ID,
                        "Insufficient evidence to determine provider outcome",
                        correlationId,
                        NOW.minusSeconds(5));

        ReconciliationOperationResult result = service.reject(request);

        assertThat(fixture.reconciliationCase().getStatus())
                .isEqualTo(ReconciliationStatus.REJECTED);

        assertThat(fixture.reconciliationCase().getResolution()).isNull();

        assertThat(fixture.reconciliationCase().getResolvedAt()).isEqualTo(NOW);

        /*
         * Rejecting a reconciliation case is not a financial result.
         */
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(fixture.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getCompletedAt()).isNull();

        assertThat(result.reconciliationStatus()).isEqualTo(ReconciliationStatus.REJECTED);

        assertThat(result.resolution()).isNull();

        assertThat(result.refundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(result.transactionStatus()).isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        ArgumentCaptor<FinancialAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(FinancialAuditRecord.class);

        verify(auditRepository).save(auditCaptor.capture());

        FinancialAuditRecord audit = auditCaptor.getValue();

        assertThat(audit.getAction()).isEqualTo(FinancialAuditAction.RECONCILIATION_REJECTED);

        assertThat(audit.getActorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(audit.getActorId()).isEqualTo(ACTOR_ID);

        assertThat(audit.getCorrelationId()).isEqualTo(correlationId);

        assertThat(audit.getOccurredAt()).isEqualTo(NOW);

        verifyLockOrder(fixture);
    }

    @Test
    void resolveTerminalCaseShouldBeRejectedWithoutAudit() {

        Fixture fixture = fixture("provider-refund-pending-123", null, null);

        fixture.reconciliationCase()
                .reject(
                        FinancialAuditActorType.USER,
                        "previous-admin",
                        "Previously rejected",
                        NOW.minusMinutes(1));

        stubLockedEntities(fixture);

        ReconciliationResolveRequest request =
                resolveRequest(
                        fixture,
                        ReconciliationResolution.REFUND_SUCCEEDED,
                        "provider-refund-final-123",
                        null,
                        null);

        assertThatThrownBy(() -> service.resolve(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.RECONCILIATION_CASE_NOT_OPEN));

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    @Test
    void resolveWhenTransactionIsNotPendingProviderShouldBeRejectedWithoutAudit() {

        Fixture fixture = fixture("provider-refund-pending-123", null, null);

        /*
         * Move transaction away from PENDING_PROVIDER without changing
         * the reconciliation case. This simulates inconsistent aggregate state.
         */
        fixture.transaction()
                .completeReconciledRefundSuccess(
                        "provider-refund-already-terminal", NOW.minusMinutes(1));

        stubLockedEntities(fixture);

        ReconciliationResolveRequest request =
                resolveRequest(
                        fixture,
                        ReconciliationResolution.REFUND_SUCCEEDED,
                        "provider-refund-final-123",
                        null,
                        null);

        assertThatThrownBy(() -> service.resolve(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .RECONCILIATION_TRANSACTION_NOT_PENDING));

        assertThat(fixture.payment().getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(fixture.reconciliationCase().getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    @Test
    void resolveWhenCaseAndTransactionPaymentDoNotMatchShouldRejectWithoutAudit() {

        Fixture fixture = fixture("provider-refund-pending-123", null, null);

        PaymentTransaction differentTransaction =
                createPendingRefundTransaction(
                        UuidGenerator.next(), "MOCK", "provider-refund-other-payment");

        when(reconciliationCaseRepository.findLockReferenceById(
                        fixture.reconciliationCase().getId()))
                .thenReturn(
                        Optional.of(
                                new ReconciliationCaseLockReference(
                                        fixture.payment().getId(), fixture.transaction().getId())));

        when(reconciliationCaseRepository.findByIdForUpdate(fixture.reconciliationCase().getId()))
                .thenReturn(Optional.of(fixture.reconciliationCase()));

        when(paymentRepository.findByIdForUpdate(fixture.payment().getId()))
                .thenReturn(Optional.of(fixture.payment()));

        when(transactionRepository.findByIdForUpdate(
                        fixture.reconciliationCase().getPaymentTransactionId()))
                .thenReturn(Optional.of(differentTransaction));

        ReconciliationResolveRequest request =
                resolveRequest(
                        fixture,
                        ReconciliationResolution.REFUND_SUCCEEDED,
                        "provider-refund-final-123",
                        null,
                        null);

        assertThatThrownBy(() -> service.resolve(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.RECONCILIATION_DATA_MISMATCH));

        verify(auditRepository, never()).save(any(FinancialAuditRecord.class));
    }

    private void stubLockedEntities(Fixture fixture) {

        when(reconciliationCaseRepository.findLockReferenceById(
                        fixture.reconciliationCase().getId()))
                .thenReturn(
                        Optional.of(
                                new ReconciliationCaseLockReference(
                                        fixture.payment().getId(), fixture.transaction().getId())));

        when(paymentRepository.findByIdForUpdate(fixture.payment().getId()))
                .thenReturn(Optional.of(fixture.payment()));

        when(transactionRepository.findByIdForUpdate(fixture.transaction().getId()))
                .thenReturn(Optional.of(fixture.transaction()));

        when(reconciliationCaseRepository.findByIdForUpdate(fixture.reconciliationCase().getId()))
                .thenReturn(Optional.of(fixture.reconciliationCase()));
    }

    private void verifyLockOrder(Fixture fixture) {

        InOrder lockOrder =
                inOrder(reconciliationCaseRepository, paymentRepository, transactionRepository);

        lockOrder
                .verify(reconciliationCaseRepository)
                .findLockReferenceById(fixture.reconciliationCase().getId());

        lockOrder.verify(paymentRepository).findByIdForUpdate(fixture.payment().getId());

        lockOrder.verify(transactionRepository).findByIdForUpdate(fixture.transaction().getId());

        lockOrder
                .verify(reconciliationCaseRepository)
                .findByIdForUpdate(fixture.reconciliationCase().getId());
    }

    private static ReconciliationResolveRequest resolveRequest(
            Fixture fixture,
            ReconciliationResolution resolution,
            String providerReference,
            String failureCode,
            String failureMessage) {

        return new ReconciliationResolveRequest(
                fixture.reconciliationCase().getId(),
                resolution,
                FinancialAuditActorType.USER,
                ACTOR_ID,
                "Manual reconciliation",
                providerReference,
                failureCode,
                failureMessage,
                UuidGenerator.next(),
                NOW.minusSeconds(5));
    }

    private static Fixture fixture(
            String pendingProviderReference,
            String pendingFailureCode,
            String pendingFailureMessage) {

        OffsetDateTime paymentRequestedAt = NOW.minusMinutes(10);

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOCK",
                        NOW.plusMinutes(10),
                        paymentRequestedAt,
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderSuccess("provider-charge-123", NOW.minusMinutes(5));

        payment.requestRefund();

        PaymentTransaction transaction =
                createPendingRefundTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        pendingProviderReference,
                        pendingFailureCode,
                        pendingFailureMessage);

        ReconciliationReason reason =
                pendingFailureCode == null
                        ? ReconciliationReason.REFUND_PROVIDER_PENDING
                        : ReconciliationReason.REFUND_PROVIDER_UNKNOWN;

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        reason,
                        payment.getProvider(),
                        transaction.getProviderReference(),
                        NOW.minusMinutes(1));

        return new Fixture(payment, transaction, reconciliationCase);
    }

    private static PaymentTransaction createPendingRefundTransaction(
            UUID paymentId, String provider, String providerReference) {

        return createPendingRefundTransaction(paymentId, provider, providerReference, null, null);
    }

    private static PaymentTransaction createPendingRefundTransaction(
            UUID paymentId,
            String provider,
            String providerReference,
            String failureCode,
            String failureMessage) {

        PaymentTransaction transaction =
                new PaymentTransaction(
                        paymentId,
                        provider,
                        PaymentTransactionType.REFUND,
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "refund:" + paymentId,
                        NOW.minusMinutes(2));

        transaction.claim(PROCESSING_OWNER, NOW.minusMinutes(2), NOW.plusMinutes(1));

        transaction.markPendingProvider(
                PROCESSING_OWNER, providerReference, failureCode, failureMessage);

        return transaction;
    }

    private record Fixture(
            Payment payment,
            PaymentTransaction transaction,
            ReconciliationCase reconciliationCase) {}
}
