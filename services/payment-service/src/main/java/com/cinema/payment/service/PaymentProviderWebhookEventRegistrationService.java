package com.cinema.payment.service;

import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;

import java.util.UUID;

public interface PaymentProviderWebhookEventRegistrationService {

    boolean register(UUID paymentTransactionId, VerifiedProviderWebhook webhook);
}
