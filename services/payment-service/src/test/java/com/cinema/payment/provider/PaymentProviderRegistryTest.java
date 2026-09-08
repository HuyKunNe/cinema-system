package com.cinema.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;

import org.junit.jupiter.api.Test;

import java.util.List;

class PaymentProviderRegistryTest {

    @Test
    void shouldResolveProviderUsingNormalizedCode() {

        PaymentProvider mockProvider = new StubPaymentProvider("MOCK");
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(mockProvider));

        PaymentProvider resolvedProvider = registry.getRequired(" mock ");

        assertThat(resolvedProvider).isSameAs(mockProvider);
    }

    @Test
    void blankProviderCodeShouldBeRejected() {

        PaymentProviderRegistry registry =
                new PaymentProviderRegistry(List.of(new StubPaymentProvider("MOCK")));

        assertThatThrownBy(() -> registry.getRequired(" "))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_REQUIRED));
    }

    @Test
    void unsupportedProviderShouldBeRejected() {

        PaymentProviderRegistry registry =
                new PaymentProviderRegistry(List.of(new StubPaymentProvider("MOCK")));

        assertThatThrownBy(() -> registry.getRequired("VNPAY"))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_NOT_SUPPORTED));
    }

    @Test
    void duplicateProviderCodesShouldFailConfiguration() {

        PaymentProvider firstProvider = new StubPaymentProvider("MOCK");
        PaymentProvider secondProvider = new StubPaymentProvider(" mock ");

        assertThatThrownBy(
                        () -> new PaymentProviderRegistry(List.of(firstProvider, secondProvider)))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID));
    }

    @Test
    void invalidConfiguredProviderCodeShouldFailConfiguration() {

        PaymentProvider invalidProvider = new StubPaymentProvider(" ");

        assertThatThrownBy(() -> new PaymentProviderRegistry(List.of(invalidProvider)))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_CONFIGURATION_INVALID));
    }

    private record StubPaymentProvider(String providerCode) implements PaymentProvider {

        @Override
        public ProviderChargeResult initiateCharge(
                ProviderChargeCommand command, String idempotencyKey) {

            throw new UnsupportedOperationException();
        }
    }
}
