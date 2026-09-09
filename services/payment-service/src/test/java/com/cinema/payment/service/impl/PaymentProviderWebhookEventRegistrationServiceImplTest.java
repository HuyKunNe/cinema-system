package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookEventRegistrationServiceImplTest {

    private static final OffsetDateTime PROCESSED_AT = OffsetDateTime.parse("2026-09-09T10:00:00Z");

    @Mock private PaymentProviderWebhookEventRepository webhookEventRepository;

    private PaymentProviderWebhookEventRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(PROCESSED_AT.toInstant(), ZoneOffset.UTC);

        service =
                new PaymentProviderWebhookEventRegistrationServiceImpl(
                        webhookEventRepository, clock);
    }

    @Test
    void newProviderEventShouldBeRegistered() {
        UUID transactionId = UuidGenerator.next();
        VerifiedProviderWebhook webhook = succeededWebhook();

        when(webhookEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(transactionId.toString()),
                        eq("MOMO"),
                        eq("provider-event-001"),
                        eq("provider-reference-001"),
                        eq("SUCCEEDED"),
                        eq(new BigDecimal("125000.00")),
                        eq("VND"),
                        eq(webhook.occurredAt()),
                        eq(PROCESSED_AT)))
                .thenReturn(1);

        boolean registered = service.register(transactionId, webhook);

        assertThat(registered).isTrue();
    }

    @Test
    void duplicateProviderEventShouldReturnFalse() {
        UUID transactionId = UuidGenerator.next();
        VerifiedProviderWebhook webhook = succeededWebhook();

        when(webhookEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(transactionId.toString()),
                        eq("MOMO"),
                        eq("provider-event-001"),
                        eq("provider-reference-001"),
                        eq("SUCCEEDED"),
                        eq(new BigDecimal("125000.00")),
                        eq("VND"),
                        eq(webhook.occurredAt()),
                        eq(PROCESSED_AT)))
                .thenReturn(0);

        boolean registered = service.register(transactionId, webhook);

        assertThat(registered).isFalse();
    }

    @Test
    void missingTransactionIdShouldBeRejected() {
        assertThatThrownBy(() -> service.register(null, succeededWebhook()))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED));

        verifyNoInteractions(webhookEventRepository);
    }

    @Test
    void missingWebhookShouldBeRejected() {
        assertThatThrownBy(() -> service.register(UuidGenerator.next(), null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_WEBHOOK_REQUIRED));

        verifyNoInteractions(webhookEventRepository);
    }

    private static VerifiedProviderWebhook succeededWebhook() {
        return new VerifiedProviderWebhook(
                "MOMO",
                "provider-event-001",
                "provider-reference-001",
                ProviderOutcome.SUCCEEDED,
                new BigDecimal("125000.00"),
                "VND",
                OffsetDateTime.parse("2026-09-09T09:59:30Z"),
                null,
                null);
    }
}
