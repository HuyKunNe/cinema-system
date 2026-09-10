package com.cinema.payment.service;

import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

public interface PaymentProviderWebhookApplicationService {

    PaymentProviderWebhookApplicationResult apply(
            VerifiedProviderWebhook webhook);
}
