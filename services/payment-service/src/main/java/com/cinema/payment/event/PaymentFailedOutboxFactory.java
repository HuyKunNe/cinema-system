package com.cinema.payment.event;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.payment.entity.Payment;

public interface PaymentFailedOutboxFactory {

    OutboxEventEntity create(Payment payment);
}
