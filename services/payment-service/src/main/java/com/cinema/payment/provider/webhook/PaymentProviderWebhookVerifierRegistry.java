package com.cinema.payment.provider.webhook;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentProviderWebhookVerifierRegistry {

    private static final int MAXIMUM_PROVIDER_CODE_LENGTH = 50;

    private final Map<String, PaymentProviderWebhookVerifier> verifiersByProvider;

    public PaymentProviderWebhookVerifierRegistry(List<PaymentProviderWebhookVerifier> verifiers) {
        if (verifiers == null) {
            throw new InternalServerException(
                    PaymentErrorCode.PROVIDER_WEBHOOK_CONFIGURATION_INVALID);
        }

        Map<String, PaymentProviderWebhookVerifier> registeredVerifiers = new LinkedHashMap<>();

        for (PaymentProviderWebhookVerifier verifier : verifiers) {
            if (verifier == null) {
                throw new InternalServerException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_CONFIGURATION_INVALID);
            }

            String providerCode = normalizeConfiguredProviderCode(verifier.providerCode());

            PaymentProviderWebhookVerifier existingVerifier =
                    registeredVerifiers.putIfAbsent(providerCode, verifier);

            if (existingVerifier != null) {
                throw new InternalServerException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_CONFIGURATION_INVALID);
            }
        }

        this.verifiersByProvider = Map.copyOf(registeredVerifiers);
    }

    public PaymentProviderWebhookVerifier getRequired(String providerCode) {
        String normalizedProviderCode = normalizeRequestedProviderCode(providerCode);

        PaymentProviderWebhookVerifier verifier = verifiersByProvider.get(normalizedProviderCode);

        if (verifier == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_NOT_SUPPORTED);
        }

        return verifier;
    }

    private String normalizeConfiguredProviderCode(String providerCode) {
        if (!isValidProviderCode(providerCode)) {
            throw new InternalServerException(
                    PaymentErrorCode.PROVIDER_WEBHOOK_CONFIGURATION_INVALID);
        }

        return normalize(providerCode);
    }

    private String normalizeRequestedProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        if (providerCode.strip().length() > MAXIMUM_PROVIDER_CODE_LENGTH) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        return normalize(providerCode);
    }

    private boolean isValidProviderCode(String providerCode) {
        return providerCode != null
                && !providerCode.isBlank()
                && providerCode.strip().length() <= MAXIMUM_PROVIDER_CODE_LENGTH;
    }

    private String normalize(String providerCode) {
        return providerCode.strip().toUpperCase(Locale.ROOT);
    }
}
