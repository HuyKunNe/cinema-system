package com.cinema.payment.provider.webhook.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ProviderWebhookRequest {

    private static final int MAX_PROVIDER_LENGTH = 50;

    private final String provider;

    private final Map<String, List<String>> headers;

    private final byte[] rawBody;

    private final OffsetDateTime receivedAt;

    public ProviderWebhookRequest(
            String provider,
            Map<String, List<String>> headers,
            byte[] rawBody,
            OffsetDateTime receivedAt) {

        this.provider = normalizeProvider(provider);
        this.headers = immutableHeaders(headers);
        this.rawBody = copyRawBody(rawBody);
        this.receivedAt = requireReceivedAt(receivedAt);
    }

    public String provider() {

        return provider;
    }

    public Map<String, List<String>> headers() {

        return headers;
    }

    public byte[] rawBody() {

        return rawBody.clone();
    }

    public OffsetDateTime receivedAt() {

        return receivedAt;
    }

    public List<String> headerValues(String name) {

        if (name == null || name.isBlank()) {
            return List.of();
        }

        return headers.getOrDefault(normalizeHeaderName(name), List.of());
    }

    public Optional<String> firstHeader(String name) {

        return headerValues(name).stream().findFirst();
    }

    private static String normalizeProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        String normalizedProvider = provider.strip().toUpperCase(Locale.ROOT);

        if (normalizedProvider.length() > MAX_PROVIDER_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        return normalizedProvider;
    }

    private static Map<String, List<String>> immutableHeaders(Map<String, List<String>> headers) {

        if (headers == null || headers.isEmpty()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_HEADERS_REQUIRED);
        }

        Map<String, List<String>> normalizedHeaders = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {

            String normalizedName = validateAndNormalizeHeaderName(entry.getKey());

            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_HEADERS_INVALID);
            }

            List<String> targetValues =
                    new ArrayList<>(normalizedHeaders.getOrDefault(normalizedName, List.of()));

            for (String value : values) {
                if (value == null) {
                    throw new ValidationException(
                            PaymentErrorCode.PROVIDER_WEBHOOK_HEADERS_INVALID);
                }

                targetValues.add(value);
            }

            normalizedHeaders.put(normalizedName, List.copyOf(targetValues));
        }

        return Map.copyOf(normalizedHeaders);
    }

    private static String validateAndNormalizeHeaderName(String name) {

        if (name == null || name.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_HEADERS_INVALID);
        }

        return normalizeHeaderName(name);
    }

    private static String normalizeHeaderName(String name) {

        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static byte[] copyRawBody(byte[] rawBody) {

        if (rawBody == null || rawBody.length == 0) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_BODY_REQUIRED);
        }

        return rawBody.clone();
    }

    private static OffsetDateTime requireReceivedAt(OffsetDateTime receivedAt) {

        if (receivedAt == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_RECEIVED_AT_REQUIRED);
        }

        return receivedAt;
    }
}
