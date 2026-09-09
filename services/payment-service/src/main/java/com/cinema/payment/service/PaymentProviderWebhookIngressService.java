package com.cinema.payment.service;

import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

import java.util.List;
import java.util.Map;

public interface PaymentProviderWebhookIngressService {

    VerifiedProviderWebhook verifyAndParse(
            String provider, Map<String, List<String>> headers, byte[] rawBody);
}
