package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.AppliedProviderChargeResult;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentResultOutboxService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PaymentProviderResultApplicationServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-08T10:00:00Z");

    private static final String PROCESSING_OWNER = "payment-provider-operation:worker-1";

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentTransactionRepository transactionRepository;

    @Mock private PaymentResultOutboxService paymentResultOutboxService;

    private PaymentProviderResultApplicationServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);

        service =
                new PaymentProviderResultApplicationServiceImpl(
                        paymentRepository,
                        transactionRepository,
                        paymentResultOutboxService,
                        clock);
    }

    @Test
    void successfulResultBeforeHoldExpirationShouldCompletePayment() {

        Fixture fixture = fixture(NOW.plusMinutes(5));

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.SUCCEEDED, "mock-reference-1", null, null, null);

        AppliedProviderChargeResult appliedResult =
                service.apply(fixture.operation(), providerResult);

        assertThat(appliedResult.paymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(appliedResult.transactionStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(fixture.payment().getProviderReference()).isEqualTo("mock-reference-1");

        assertThat(fixture.payment().getCompletedAt()).isEqualTo(NOW);

        assertThat(fixture.transaction().getProcessingOwner()).isNull();

        assertThat(fixture.transaction().getProcessingExpiresAt()).isNull();

        verifyLockOrder(fixture);

        verify(paymentResultOutboxService).persistIfTerminal(fixture.payment());
    }

    @Test
    void lateSuccessfulResultShouldRequireReconciliation() {

        Fixture fixture = fixture(NOW);

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.SUCCEEDED, "mock-reference-late", null, null, null);

        service.apply(fixture.operation(), providerResult);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        verify(paymentResultOutboxService).persistIfTerminal(fixture.payment());
    }

    @Test
    void failedResultShouldFailPaymentAndTransaction() {

        Fixture fixture = fixture(NOW.plusMinutes(5));

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.FAILED,
                        null,
                        null,
                        "PAYMENT_DECLINED",
                        "Payment was declined");

        service.apply(fixture.operation(), providerResult);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(fixture.transaction().getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        assertThat(fixture.payment().getFailureCode()).isEqualTo("PAYMENT_DECLINED");

        assertThat(fixture.payment().getFailureMessage()).isEqualTo("Payment was declined");

        assertThat(fixture.payment().getCompletedAt()).isEqualTo(NOW);

        verify(paymentResultOutboxService).persistIfTerminal(fixture.payment());
    }

    @Test
    void pendingResultShouldPersistPendingProviderState() {

        Fixture fixture = fixture(NOW.plusMinutes(5));

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.PENDING,
                        "mock-pending-reference",
                        URI.create("https://example.test/payment"),
                        null,
                        null);

        service.apply(fixture.operation(), providerResult);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(fixture.payment().getProviderReference()).isEqualTo("mock-pending-reference");

        assertThat(fixture.payment().getCompletedAt()).isNull();

        assertThat(fixture.transaction().getCompletedAt()).isNull();

        verify(paymentResultOutboxService).persistIfTerminal(fixture.payment());
    }

    @Test
    void unknownResultShouldNotBecomeTerminalFailure() {

        Fixture fixture = fixture(NOW.plusMinutes(5));

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.UNKNOWN,
                        null,
                        null,
                        "PROVIDER_OUTCOME_UNKNOWN",
                        "Provider charge outcome could not be determined");

        service.apply(fixture.operation(), providerResult);

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(fixture.transaction().getFailureCode()).isEqualTo("PROVIDER_OUTCOME_UNKNOWN");

        assertThat(fixture.transaction().getFailureMessage())
                .isEqualTo("Provider charge outcome could not be determined");

        assertThat(fixture.payment().isTerminal()).isFalse();

        assertThat(fixture.payment().getFailureCode()).isNull();

        assertThat(fixture.payment().getCompletedAt()).isNull();

        verify(paymentResultOutboxService).persistIfTerminal(fixture.payment());
    }

    @Test
    void replacedLeaseOwnerShouldRejectStaleResult() {

        Fixture fixture = fixture(NOW.plusMinutes(5));

        fixture.transaction()
                .claim(
                        "payment-provider-operation:worker-2",
                        NOW.plusSeconds(31),
                        NOW.plusSeconds(61));

        stubLockedEntities(fixture);

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.SUCCEEDED, "mock-reference-stale", null, null, null);

        assertThatThrownBy(() -> service.apply(fixture.operation(), providerResult))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_TRANSACTION_LEASE_NOT_OWNED));

        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        verify(paymentResultOutboxService, never()).persistIfTerminal(fixture.payment());
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

    private static Fixture fixture(OffsetDateTime holdExpiresAt) {

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOCK",
                        holdExpiresAt,
                        NOW.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.CHARGE,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        "charge:" + payment.getId(),
                        payment.getRequestedAt());

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(1), NOW.plusSeconds(30));

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getHoldExpiresAt());

        ClaimedProviderChargeOperation operation =
                new ClaimedProviderChargeOperation(
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
            ClaimedProviderChargeOperation operation) {}
}
