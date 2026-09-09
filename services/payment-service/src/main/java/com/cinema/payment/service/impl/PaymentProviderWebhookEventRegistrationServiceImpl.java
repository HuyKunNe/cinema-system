package com.cinema.payment.service.impl;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;
import com.cinema.payment.service.PaymentProviderWebhookEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentProviderWebhookEventRegistrationServiceImpl
        implements PaymentProviderWebhookEventRegistrationService {

    private final PaymentProviderWebhookEventRepository webhookEventRepository;

    private final Clock clock;

    public PaymentProviderWebhookEventRegistrationServiceImpl(
            PaymentProviderWebhookEventRepository webhookEventRepository, Clock clock) {

        this.webhookEventRepository = webhookEventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean register(UUID paymentTransactionId, VerifiedProviderWebhook webhook) {

        if (paymentTransactionId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED);
        }

        if (webhook == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_REQUIRED);
        }

        int insertedRows =
                webhookEventRepository.insertIfAbsent(
                        UuidGenerator.next().toString(),
                        paymentTransactionId.toString(),
                        webhook.provider(),
                        webhook.providerEventId(),
                        webhook.providerReference(),
                        webhook.outcome().name(),
                        webhook.amount(),
                        webhook.currency(),
                        webhook.occurredAt(),
                        OffsetDateTime.now(clock));

        return insertedRows == 1;
    }
}
