package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookApplicationDisposition;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderWebhookEventRegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookApplicationServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-09T11:00:00Z");

    private static final String PROVIDER_REFERENCE = "provider-reference-001";

    private static final String PROCESSING_OWNER = "payment-webhook-test-worker";

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentTransactionRepository transactionRepository;

    @Mock private PaymentProviderWebhookEventRegistrationService eventRegistrationService;

    private PaymentProviderWebhookApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);

        service =
                new PaymentProviderWebhookApplicationServiceImpl(
                        paymentRepository, transactionRepository, eventRegistrationService, clock);
    }

    @Test
    void successWebhookShouldUseLockOrderAndApplyAtomically() {
        TestAggregate aggregate = pendingAggregate();
        VerifiedProviderWebhook webhook =
                webhook(
                        "event-success-001",
                        ProviderOutcome.SUCCEEDED,
                        new BigDecimal("125000.00"),
                        null);

        prepareRepositoryLookups(aggregate, webhook);

        when(eventRegistrationService.register(aggregate.transaction().getId(), webhook))
                .thenReturn(true);

        PaymentProviderWebhookApplicationResult result = service.apply(webhook);

        assertThat(result.disposition()).isEqualTo(ProviderWebhookApplicationDisposition.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.transactionStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(aggregate.payment().getProviderReference()).isEqualTo(PROVIDER_REFERENCE);
        assertThat(aggregate.transaction().getProviderEventId()).isEqualTo("event-success-001");

        InOrder order = inOrder(transactionRepository, paymentRepository, eventRegistrationService);

        order.verify(transactionRepository)
                .findByProviderAndProviderReference("MOMO", PROVIDER_REFERENCE);
        order.verify(paymentRepository).findByIdForUpdate(aggregate.payment().getId());
        order.verify(transactionRepository).findByIdForUpdate(aggregate.transaction().getId());
        order.verify(eventRegistrationService).register(aggregate.transaction().getId(), webhook);
    }

    @Test
    void duplicateWebhookShouldNotApplyStateAgain() {
        TestAggregate aggregate = pendingAggregate();
        VerifiedProviderWebhook webhook =
                webhook(
                        "event-duplicate-001",
                        ProviderOutcome.SUCCEEDED,
                        new BigDecimal("125000.00"),
                        null);

        prepareRepositoryLookups(aggregate, webhook);

        when(eventRegistrationService.register(aggregate.transaction().getId(), webhook))
                .thenReturn(false);

        PaymentProviderWebhookApplicationResult result = service.apply(webhook);

        assertThat(result.disposition()).isEqualTo(ProviderWebhookApplicationDisposition.DUPLICATE);
        assertThat(aggregate.payment().getStatus()).isEqualTo(PaymentStatus.PENDING_PROVIDER);
        assertThat(aggregate.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);
        assertThat(aggregate.transaction().getProviderEventId()).isNull();
    }

    @Test
    void failureWebhookShouldApplyTerminalFailure() {
        TestAggregate aggregate = pendingAggregate();
        VerifiedProviderWebhook webhook =
                webhook(
                        "event-failure-001",
                        ProviderOutcome.FAILED,
                        new BigDecimal("125000.00"),
                        "PAYMENT_DECLINED");

        prepareRepositoryLookups(aggregate, webhook);

        when(eventRegistrationService.register(aggregate.transaction().getId(), webhook))
                .thenReturn(true);

        PaymentProviderWebhookApplicationResult result = service.apply(webhook);

        assertThat(result.disposition()).isEqualTo(ProviderWebhookApplicationDisposition.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.transactionStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(aggregate.payment().getFailureCode()).isEqualTo("PAYMENT_DECLINED");
        assertThat(aggregate.transaction().getProviderEventId()).isEqualTo("event-failure-001");
    }

    @Test
    void stalePendingWebhookShouldNotRegressSuccessfulPayment() {
        TestAggregate aggregate = pendingAggregate();

        aggregate
                .transaction()
                .completeSuccessfullyFromWebhook(
                        "existing-success-event", PROVIDER_REFERENCE, NOW.minusSeconds(5));

        aggregate.payment().completeProviderSuccess(PROVIDER_REFERENCE, NOW.minusSeconds(5));

        VerifiedProviderWebhook webhook =
                webhook(
                        "late-pending-event",
                        ProviderOutcome.PENDING,
                        new BigDecimal("125000.00"),
                        null);

        prepareRepositoryLookups(aggregate, webhook);

        when(eventRegistrationService.register(aggregate.transaction().getId(), webhook))
                .thenReturn(true);

        PaymentProviderWebhookApplicationResult result = service.apply(webhook);

        assertThat(result.disposition())
                .isEqualTo(ProviderWebhookApplicationDisposition.IGNORED_STALE);
        assertThat(aggregate.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(aggregate.transaction().getStatus())
                .isEqualTo(PaymentTransactionStatus.SUCCEEDED);
        assertThat(aggregate.transaction().getProviderEventId())
                .isEqualTo("existing-success-event");
    }

    @Test
    void mismatchedAmountShouldFailBeforeEventRegistration() {
        TestAggregate aggregate = pendingAggregate();
        VerifiedProviderWebhook webhook =
                webhook(
                        "event-mismatch-001",
                        ProviderOutcome.SUCCEEDED,
                        new BigDecimal("130000.00"),
                        null);

        prepareRepositoryLookups(aggregate, webhook);

        assertThatThrownBy(() -> service.apply(webhook))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_TRANSACTION_DATA_MISMATCH));

        verify(eventRegistrationService, never())
                .register(aggregate.transaction().getId(), webhook);
    }

    private void prepareRepositoryLookups(
            TestAggregate aggregate, VerifiedProviderWebhook webhook) {

        when(transactionRepository.findByProviderAndProviderReference(
                        webhook.provider(), webhook.providerReference()))
                .thenReturn(Optional.of(aggregate.transaction()));

        when(paymentRepository.findByIdForUpdate(aggregate.payment().getId()))
                .thenReturn(Optional.of(aggregate.payment()));

        when(transactionRepository.findByIdForUpdate(aggregate.transaction().getId()))
                .thenReturn(Optional.of(aggregate.transaction()));
    }

    private static TestAggregate pendingAggregate() {
        UUID bookingId = UuidGenerator.next();
        UUID userId = UuidGenerator.next();
        UUID sourceEventId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();

        Payment payment =
                new Payment(
                        bookingId,
                        userId,
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOMO",
                        NOW.plusMinutes(10),
                        NOW.minusMinutes(1),
                        sourceEventId,
                        correlationId);

        payment.startProcessing();

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        "MOMO",
                        PaymentTransactionType.CHARGE,
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "charge:" + payment.getId(),
                        NOW.minusMinutes(1));

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(20), NOW.plusMinutes(1));

        transaction.markPendingProvider(PROCESSING_OWNER, PROVIDER_REFERENCE, null, null);

        payment.recordPendingProvider(PROVIDER_REFERENCE, NOW.minusSeconds(10));

        return new TestAggregate(payment, transaction);
    }

    private static VerifiedProviderWebhook webhook(
            String eventId, ProviderOutcome outcome, BigDecimal amount, String failureCode) {

        return new VerifiedProviderWebhook(
                "MOMO",
                eventId,
                PROVIDER_REFERENCE,
                outcome,
                amount,
                "VND",
                NOW.minusSeconds(1),
                failureCode,
                failureCode == null ? null : "Provider declined payment");
    }

    private record TestAggregate(Payment payment, PaymentTransaction transaction) {}
}
