package com.cinema.payment.provider.webhook.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ProviderWebhookRequestTest {

    private static final OffsetDateTime RECEIVED_AT = OffsetDateTime.parse("2026-09-08T11:00:00Z");

    @Test
    void shouldDefensivelyCopyHeadersAndRawBody() {

        List<String> signatureValues = new ArrayList<>(List.of("signature-1"));

        Map<String, List<String>> headers = new LinkedHashMap<>();

        headers.put("X-Provider-Signature", signatureValues);

        byte[] rawBody = "{\"result\":\"success\"}".getBytes(StandardCharsets.UTF_8);

        ProviderWebhookRequest request =
                new ProviderWebhookRequest(" mock ", headers, rawBody, RECEIVED_AT);

        signatureValues.set(0, "modified-signature");
        headers.put("X-New-Header", List.of("new-value"));
        rawBody[0] = 'X';

        assertThat(request.provider()).isEqualTo("MOCK");

        assertThat(request.firstHeader("x-provider-signature")).contains("signature-1");

        assertThat(request.headers()).doesNotContainKey("x-new-header");

        assertThat(new String(request.rawBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"result\":\"success\"}");
    }

    @Test
    void returnedCollectionsAndBodyShouldNotMutateRequest() {

        ProviderWebhookRequest request = request();

        assertThatThrownBy(() -> request.headers().put("another-header", List.of("value")))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(
                        () -> request.headerValues("x-provider-signature").add("another-signature"))
                .isInstanceOf(UnsupportedOperationException.class);

        byte[] returnedBody = request.rawBody();
        returnedBody[0] = 'X';

        assertThat(new String(request.rawBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"payment\":\"accepted\"}");
    }

    @Test
    void headerLookupShouldBeCaseInsensitive() {

        ProviderWebhookRequest request = request();

        assertThat(request.firstHeader("X-PROVIDER-SIGNATURE")).contains("signature-1");
    }

    @Test
    void missingHeadersShouldBeRejected() {

        assertValidation(
                () ->
                        new ProviderWebhookRequest(
                                "MOCK",
                                Map.of(),
                                "{}".getBytes(StandardCharsets.UTF_8),
                                RECEIVED_AT),
                PaymentErrorCode.PROVIDER_WEBHOOK_HEADERS_REQUIRED);
    }

    @Test
    void missingRawBodyShouldBeRejected() {

        assertValidation(
                () ->
                        new ProviderWebhookRequest(
                                "MOCK",
                                Map.of("x-provider-signature", List.of("signature-1")),
                                new byte[0],
                                RECEIVED_AT),
                PaymentErrorCode.PROVIDER_WEBHOOK_BODY_REQUIRED);
    }

    private static ProviderWebhookRequest request() {

        return new ProviderWebhookRequest(
                "MOCK",
                Map.of("X-Provider-Signature", List.of("signature-1")),
                "{\"payment\":\"accepted\"}".getBytes(StandardCharsets.UTF_8),
                RECEIVED_AT);
    }

    private static void assertValidation(
            ThrowingCallable callable, PaymentErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}

