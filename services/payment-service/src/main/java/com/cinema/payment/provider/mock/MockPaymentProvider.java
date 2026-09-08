package com.cinema.payment.provider.mock;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.PaymentProvider;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cinema.payment.provider",
        havingValue = MockPaymentProvider.PROVIDER_CODE,
        matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    public static final String PROVIDER_CODE = "MOCK";

    private static final String PROVIDER_REFERENCE_PREFIX = "mock-";

    @Override
    public String providerCode() {

        return PROVIDER_CODE;
    }

    @Override
    public ProviderChargeResult initiateCharge(
            ProviderChargeCommand command, String idempotencyKey) {

        if (command == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_CHARGE_COMMAND_REQUIRED);
        }

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        return ProviderChargeResult.succeeded(PROVIDER_REFERENCE_PREFIX + normalizedIdempotencyKey);
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String normalizedIdempotencyKey = idempotencyKey.strip();

        if (normalizedIdempotencyKey.length() > 200) {
            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_INVALID);
        }

        return normalizedIdempotencyKey;
    }
}
