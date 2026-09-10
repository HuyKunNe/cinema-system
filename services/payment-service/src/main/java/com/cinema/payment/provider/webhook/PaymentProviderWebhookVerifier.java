package com.cinema.payment.provider.webhook;

import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.provider.webhook.model.ProviderWebhookRequest;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

public interface PaymentProviderWebhookVerifier {

    String providerCode();

    VerifiedProviderWebhook verifyAndParse(ProviderWebhookRequest request);

    ProviderWebhookAcknowledgement acknowledgement(PaymentProviderWebhookApplicationResult result);
}
