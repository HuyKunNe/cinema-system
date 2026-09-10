package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.event.PaymentFailedOutboxFactory;
import com.cinema.payment.event.PaymentSucceededOutboxFactory;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.service.PaymentResultOutboxService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentResultOutboxServiceImpl implements PaymentResultOutboxService {

    private final PaymentSucceededOutboxFactory succeededOutboxFactory;

    private final PaymentFailedOutboxFactory failedOutboxFactory;

    private final OutboxRepository outboxRepository;

    public PaymentResultOutboxServiceImpl(
            PaymentSucceededOutboxFactory succeededOutboxFactory,
            PaymentFailedOutboxFactory failedOutboxFactory,
            OutboxRepository outboxRepository) {

        this.succeededOutboxFactory = succeededOutboxFactory;
        this.failedOutboxFactory = failedOutboxFactory;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void persistIfTerminal(Payment payment) {

        if (payment == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_REQUIRED);
        }

        OutboxEventEntity event =
                switch (payment.getStatus()) {
                    case SUCCEEDED -> succeededOutboxFactory.create(payment);

                    case FAILED, EXPIRED -> failedOutboxFactory.create(payment);

                    case RECEIVED, PROCESSING, PENDING_PROVIDER, RECONCILIATION_REQUIRED -> null;
                };

        if (event != null) {
            outboxRepository.save(event);
        }
    }
}
