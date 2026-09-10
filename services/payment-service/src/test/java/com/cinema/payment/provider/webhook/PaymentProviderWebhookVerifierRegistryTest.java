package com.cinema.payment.provider.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.provider.webhook.model.ProviderWebhookRequest;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

import org.junit.jupiter.api.Test;

import java.util.List;

class PaymentProviderWebhookVerifierRegistryTest {

    @Test
    void getRequiredShouldResolveNormalizedProviderCode() {
        PaymentProviderWebhookVerifier momoVerifier = new StubWebhookVerifier("MOMO");

        PaymentProviderWebhookVerifierRegistry registry =
                new PaymentProviderWebhookVerifierRegistry(List.of(momoVerifier));

        PaymentProviderWebhookVerifier resolved = registry.getRequired(" momo ");

        assertThat(resolved).isSameAs(momoVerifier);
    }

    @Test
    void getRequiredShouldRejectUnsupportedProvider() {
        PaymentProviderWebhookVerifierRegistry registry =
                new PaymentProviderWebhookVerifierRegistry(List.of());

        assertThatThrownBy(() -> registry.getRequired("VNPAY"))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_WEBHOOK_NOT_SUPPORTED));
    }

    @Test
    void constructorShouldRejectDuplicateNormalizedProviderCodes() {
        PaymentProviderWebhookVerifier firstVerifier = new StubWebhookVerifier("MOMO");

        PaymentProviderWebhookVerifier secondVerifier = new StubWebhookVerifier(" momo ");

        assertThatThrownBy(
                        () ->
                                new PaymentProviderWebhookVerifierRegistry(
                                        List.of(firstVerifier, secondVerifier)))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PROVIDER_WEBHOOK_CONFIGURATION_INVALID));
    }

    @Test
    void constructorShouldRejectInvalidConfiguredProviderCode() {
        PaymentProviderWebhookVerifier verifier = new StubWebhookVerifier(" ");

        assertThatThrownBy(() -> new PaymentProviderWebhookVerifierRegistry(List.of(verifier)))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PROVIDER_WEBHOOK_CONFIGURATION_INVALID));
    }

    @Test
    void getRequiredShouldRejectMissingProviderCode() {
        PaymentProviderWebhookVerifierRegistry registry =
                new PaymentProviderWebhookVerifierRegistry(List.of());

        assertThatThrownBy(() -> registry.getRequired(" "))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_REQUIRED));
    }

    private record StubWebhookVerifier(String providerCode)
            implements PaymentProviderWebhookVerifier {

        @Override
        public VerifiedProviderWebhook verifyAndParse(ProviderWebhookRequest request) {
            throw new UnsupportedOperationException(
                    "Verification is not required by this registry unit test");
        }

        @Override
        public ProviderWebhookAcknowledgement acknowledgement(
                PaymentProviderWebhookApplicationResult result) {

            return ProviderWebhookAcknowledgement.noContent();
        }
    }
}
