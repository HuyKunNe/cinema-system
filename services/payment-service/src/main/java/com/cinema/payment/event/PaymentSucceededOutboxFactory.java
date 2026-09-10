package com.cinema.payment.event;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.payment.entity.Payment;

public interface PaymentSucceededOutboxFactory {

    OutboxEventEntity create(Payment payment);
}
