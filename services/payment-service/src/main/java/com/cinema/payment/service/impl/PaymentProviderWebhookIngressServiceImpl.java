package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifier;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifierRegistry;
import com.cinema.payment.provider.webhook.ProviderWebhookBodySizeValidator;
import com.cinema.payment.provider.webhook.model.ProviderWebhookRequest;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.service.PaymentProviderWebhookIngressService;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentProviderWebhookIngressServiceImpl
        implements PaymentProviderWebhookIngressService {

    private final ProviderWebhookBodySizeValidator bodySizeValidator;

    private final PaymentProviderWebhookVerifierRegistry verifierRegistry;

    private final Clock clock;

    public PaymentProviderWebhookIngressServiceImpl(
            ProviderWebhookBodySizeValidator bodySizeValidator,
            PaymentProviderWebhookVerifierRegistry verifierRegistry,
            Clock clock) {

        this.bodySizeValidator = bodySizeValidator;
        this.verifierRegistry = verifierRegistry;
        this.clock = clock;
    }

    @Override
    public VerifiedProviderWebhook verifyAndParse(
            String provider, Map<String, List<String>> headers, byte[] rawBody) {

        OffsetDateTime receivedAt = OffsetDateTime.now(clock);

        bodySizeValidator.validate(rawBody);

        PaymentProviderWebhookVerifier verifier = verifierRegistry.getRequired(provider);

        ProviderWebhookRequest request =
                new ProviderWebhookRequest(provider, headers, rawBody, receivedAt);

        VerifiedProviderWebhook verifiedWebhook = verifier.verifyAndParse(request);

        validateVerifierResult(request, verifiedWebhook);

        return verifiedWebhook;
    }

    private static void validateVerifierResult(
            ProviderWebhookRequest request, VerifiedProviderWebhook verifiedWebhook) {

        if (verifiedWebhook == null || !request.provider().equals(verifiedWebhook.provider())) {

            throw new InternalServerException(
                    PaymentErrorCode.PROVIDER_WEBHOOK_VERIFIER_RESULT_INVALID);
        }
    }
}
