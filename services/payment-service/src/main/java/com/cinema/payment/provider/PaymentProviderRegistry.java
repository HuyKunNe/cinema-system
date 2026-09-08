package com.cinema.payment.provider;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProvider> providersByCode;

    public PaymentProviderRegistry(List<PaymentProvider> providers) {

        if (providers == null) {
            throw new InternalServerException(PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID);
        }

        Map<String, PaymentProvider> registeredProviders = new LinkedHashMap<>();

        for (PaymentProvider provider : providers) {
            if (provider == null) {
                throw new InternalServerException(PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID);
            }

            String providerCode = normalizeConfiguredProviderCode(provider.providerCode());

            PaymentProvider existingProvider =
                    registeredProviders.putIfAbsent(providerCode, provider);

            if (existingProvider != null) {
                throw new InternalServerException(PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID);
            }
        }

        this.providersByCode = Map.copyOf(registeredProviders);
    }

    public PaymentProvider getRequired(String providerCode) {

        String normalizedProviderCode = normalizeRequestedProviderCode(providerCode);

        PaymentProvider provider = providersByCode.get(normalizedProviderCode);

        if (provider == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_NOT_SUPPORTED);
        }

        return provider;
    }

    private static String normalizeConfiguredProviderCode(String providerCode) {

        if (providerCode == null || providerCode.isBlank()) {
            throw new InternalServerException(PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID);
        }

        String normalizedProviderCode = providerCode.strip().toUpperCase(Locale.ROOT);

        if (normalizedProviderCode.length() > 50) {
            throw new InternalServerException(PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID);
        }

        return normalizedProviderCode;
    }

    private static String normalizeRequestedProviderCode(String providerCode) {

        if (providerCode == null || providerCode.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REQUIRED);
        }

        String normalizedProviderCode = providerCode.strip().toUpperCase(Locale.ROOT);

        if (normalizedProviderCode.length() > 50) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_INVALID);
        }

        return normalizedProviderCode;
    }
}
