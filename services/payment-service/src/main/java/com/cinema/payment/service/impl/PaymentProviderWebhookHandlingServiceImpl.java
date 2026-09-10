package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifier;
import com.cinema.payment.provider.webhook.PaymentProviderWebhookVerifierRegistry;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.service.PaymentProviderWebhookApplicationService;
import com.cinema.payment.service.PaymentProviderWebhookHandlingService;
import com.cinema.payment.service.PaymentProviderWebhookIngressService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PaymentProviderWebhookHandlingServiceImpl
        implements PaymentProviderWebhookHandlingService {

    private final PaymentProviderWebhookIngressService ingressService;

    private final PaymentProviderWebhookApplicationService applicationService;

    private final PaymentProviderWebhookVerifierRegistry verifierRegistry;

    public PaymentProviderWebhookHandlingServiceImpl(
            PaymentProviderWebhookIngressService ingressService,
            PaymentProviderWebhookApplicationService applicationService,
            PaymentProviderWebhookVerifierRegistry verifierRegistry) {

        this.ingressService = ingressService;
        this.applicationService = applicationService;
        this.verifierRegistry = verifierRegistry;
    }

    @Override
    public ProviderWebhookAcknowledgement handle(
            String provider, Map<String, List<String>> headers, byte[] rawBody) {

        VerifiedProviderWebhook webhook = ingressService.verifyAndParse(provider, headers, rawBody);

        PaymentProviderWebhookApplicationResult result = applicationService.apply(webhook);

        PaymentProviderWebhookVerifier verifier = verifierRegistry.getRequired(webhook.provider());

        ProviderWebhookAcknowledgement acknowledgement = verifier.acknowledgement(result);

        if (acknowledgement == null) {
            throw new InternalServerException(
                    PaymentErrorCode.PROVIDER_WEBHOOK_ACKNOWLEDGEMENT_INVALID);
        }

        return acknowledgement;
    }
}
