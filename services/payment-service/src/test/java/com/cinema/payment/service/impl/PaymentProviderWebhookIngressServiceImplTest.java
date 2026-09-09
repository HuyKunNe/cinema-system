package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifier;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifierRegistry;
import com.cinema.payment.provider.webhook.ProviderWebhookBodySizeValidator;
import com.cinema.payment.provider.webhook.model.ProviderWebhookRequest;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookIngressServiceImplTest {

    private static final Instant RECEIVED_INSTANT = Instant.parse("2026-09-09T09:30:00Z");

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-09T09:29:30Z");

    @Mock private ProviderWebhookBodySizeValidator bodySizeValidator;

    @Mock private PaymentProviderWebhookVerifierRegistry verifierRegistry;

    @Mock private PaymentProviderWebhookVerifier verifier;

    private PaymentProviderWebhookIngressServiceImpl ingressService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(RECEIVED_INSTANT, ZoneOffset.UTC);

        ingressService =
                new PaymentProviderWebhookIngressServiceImpl(
                        bodySizeValidator, verifierRegistry, clock);
    }

    @Test
    void verifyAndParseShouldUseTrustedBoundaryOrder() {
        byte[] rawBody = "{\"resultCode\":0}".getBytes(StandardCharsets.UTF_8);

        Map<String, List<String>> headers = Map.of("X-Signature", List.of("test-signature"));

        VerifiedProviderWebhook verifiedWebhook = succeededWebhook("MOMO");

        when(verifierRegistry.getRequired(" momo ")).thenReturn(verifier);

        when(verifier.verifyAndParse(any(ProviderWebhookRequest.class)))
                .thenReturn(verifiedWebhook);

        VerifiedProviderWebhook result = ingressService.verifyAndParse(" momo ", headers, rawBody);

        assertThat(result).isSameAs(verifiedWebhook);

        ArgumentCaptor<ProviderWebhookRequest> requestCaptor =
                ArgumentCaptor.forClass(ProviderWebhookRequest.class);

        InOrder order = inOrder(bodySizeValidator, verifierRegistry, verifier);

        order.verify(bodySizeValidator).validate(rawBody);
        order.verify(verifierRegistry).getRequired(" momo ");
        order.verify(verifier).verifyAndParse(requestCaptor.capture());

        ProviderWebhookRequest request = requestCaptor.getValue();

        assertThat(request.provider()).isEqualTo("MOMO");
        assertThat(request.receivedAt())
                .isEqualTo(OffsetDateTime.ofInstant(RECEIVED_INSTANT, ZoneOffset.UTC));
        assertThat(request.rawBody()).containsExactly(rawBody);
        assertThat(request.firstHeader("x-signature")).contains("test-signature");
    }

    @Test
    void oversizedBodyShouldBeRejectedBeforeVerifierResolution() {
        byte[] rawBody = new byte[65_537];

        ValidationException bodySizeFailure =
                new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_BODY_TOO_LARGE);

        doThrow(bodySizeFailure).when(bodySizeValidator).validate(rawBody);

        assertThatThrownBy(() -> ingressService.verifyAndParse("MOMO", validHeaders(), rawBody))
                .isSameAs(bodySizeFailure);

        verifyNoInteractions(verifierRegistry, verifier);
    }

    @Test
    void unsupportedProviderShouldNotInvokeVerifier() {
        byte[] rawBody = validRawBody();

        ValidationException unsupportedProviderFailure =
                new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_NOT_SUPPORTED);

        when(verifierRegistry.getRequired("UNKNOWN")).thenThrow(unsupportedProviderFailure);

        assertThatThrownBy(() -> ingressService.verifyAndParse("UNKNOWN", validHeaders(), rawBody))
                .isSameAs(unsupportedProviderFailure);

        verifyNoInteractions(verifier);
    }

    @Test
    void verifierValidationFailureShouldBePreserved() {
        byte[] rawBody = validRawBody();

        ValidationException verificationFailure =
                new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID);

        when(verifierRegistry.getRequired("MOMO")).thenReturn(verifier);

        when(verifier.verifyAndParse(any(ProviderWebhookRequest.class)))
                .thenThrow(verificationFailure);

        assertThatThrownBy(() -> ingressService.verifyAndParse("MOMO", validHeaders(), rawBody))
                .isSameAs(verificationFailure);
    }

    @Test
    void nullVerifierResultShouldBeRejectedAsSystemFailure() {
        byte[] rawBody = validRawBody();

        when(verifierRegistry.getRequired("MOMO")).thenReturn(verifier);

        when(verifier.verifyAndParse(any(ProviderWebhookRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> ingressService.verifyAndParse("MOMO", validHeaders(), rawBody))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PROVIDER_WEBHOOK_VERIFIER_RESULT_INVALID));
    }

    @Test
    void verifierResultForDifferentProviderShouldBeRejected() {
        byte[] rawBody = validRawBody();

        when(verifierRegistry.getRequired("MOMO")).thenReturn(verifier);

        when(verifier.verifyAndParse(any(ProviderWebhookRequest.class)))
                .thenReturn(succeededWebhook("VNPAY"));

        assertThatThrownBy(() -> ingressService.verifyAndParse("MOMO", validHeaders(), rawBody))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PROVIDER_WEBHOOK_VERIFIER_RESULT_INVALID));
    }

    private static VerifiedProviderWebhook succeededWebhook(String provider) {

        return new VerifiedProviderWebhook(
                provider,
                "provider-event-001",
                "provider-reference-001",
                ProviderOutcome.SUCCEEDED,
                new BigDecimal("125000.00"),
                "VND",
                OCCURRED_AT,
                null,
                null);
    }

    private static Map<String, List<String>> validHeaders() {
        return Map.of("X-Signature", List.of("test-signature"));
    }

    private static byte[] validRawBody() {
        return "{\"resultCode\":0}".getBytes(StandardCharsets.UTF_8);
    }
}
