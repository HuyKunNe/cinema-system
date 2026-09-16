package com.cinema.payment.provider.mock;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.PaymentProvider;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderRefundCommand;
import com.cinema.payment.provider.model.ProviderRefundResult;

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

    private static final String REFUND_REFERENCE_PREFIX = "mock-refund-";

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

    @Override
    public ProviderRefundResult initiateRefund(
            ProviderRefundCommand command, String idempotencyKey) {

        if (command == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFUND_COMMAND_REQUIRED);
        }

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        return ProviderRefundResult.succeeded(REFUND_REFERENCE_PREFIX + normalizedIdempotencyKey);
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {

            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String normalized = idempotencyKey.strip();

        if (normalized.length() > 200) {
            throw new ValidationException(PaymentErrorCode.IDEMPOTENCY_KEY_INVALID);
        }

        return normalized;
    }
}
