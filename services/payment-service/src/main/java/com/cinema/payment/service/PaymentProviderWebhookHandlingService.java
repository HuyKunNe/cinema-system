package com.cinema.payment.service;

import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;

import java.util.List;
import java.util.Map;

public interface PaymentProviderWebhookHandlingService {

    ProviderWebhookAcknowledgement handle(
            String provider, Map<String, List<String>> headers, byte[] rawBody);
}
