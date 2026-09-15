package com.cinema.booking.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentFailedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        PAYMENT_FAILED
    }

    record Result(Status status) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE);
        }

        public static Result paymentFailed() {

            return new Result(Status.PAYMENT_FAILED);
        }
    }
}
