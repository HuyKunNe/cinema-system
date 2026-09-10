package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifier;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifierRegistry;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.provider.webhook.model.ProviderWebhookApplicationDisposition;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.service.PaymentProviderWebhookApplicationService;
import com.cinema.payment.service.PaymentProviderWebhookIngressService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookHandlingServiceImplTest {

    @Mock private PaymentProviderWebhookIngressService ingressService;

    @Mock private PaymentProviderWebhookApplicationService applicationService;

    @Mock private PaymentProviderWebhookVerifierRegistry verifierRegistry;

    @Mock private PaymentProviderWebhookVerifier verifier;

    private PaymentProviderWebhookHandlingServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new PaymentProviderWebhookHandlingServiceImpl(
                        ingressService, applicationService, verifierRegistry);
    }

    @Test
    void handleShouldVerifyApplyAndAcknowledgeInOrder() {
        byte[] rawBody = "{\"resultCode\":0}".getBytes(StandardCharsets.UTF_8);

        Map<String, List<String>> headers = Map.of("X-Signature", List.of("test-signature"));

        VerifiedProviderWebhook webhook =
                new VerifiedProviderWebhook(
                        "MOMO",
                        "provider-event-001",
                        "provider-reference-001",
                        ProviderOutcome.SUCCEEDED,
                        new BigDecimal("125000.00"),
                        "VND",
                        OffsetDateTime.parse("2026-09-10T10:00:00Z"),
                        null,
                        null);

        PaymentProviderWebhookApplicationResult applicationResult =
                new PaymentProviderWebhookApplicationResult(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        PaymentStatus.SUCCEEDED,
                        PaymentTransactionStatus.SUCCEEDED,
                        ProviderWebhookApplicationDisposition.APPLIED);

        ProviderWebhookAcknowledgement acknowledgement = ProviderWebhookAcknowledgement.noContent();

        when(ingressService.verifyAndParse("MOMO", headers, rawBody)).thenReturn(webhook);

        when(applicationService.apply(webhook)).thenReturn(applicationResult);

        when(verifierRegistry.getRequired("MOMO")).thenReturn(verifier);

        when(verifier.acknowledgement(applicationResult)).thenReturn(acknowledgement);

        ProviderWebhookAcknowledgement result = service.handle("MOMO", headers, rawBody);

        assertThat(result).isSameAs(acknowledgement);

        InOrder order = inOrder(ingressService, applicationService, verifierRegistry, verifier);

        order.verify(ingressService).verifyAndParse("MOMO", headers, rawBody);

        order.verify(applicationService).apply(webhook);

        order.verify(verifierRegistry).getRequired("MOMO");

        order.verify(verifier).acknowledgement(applicationResult);
    }
}
