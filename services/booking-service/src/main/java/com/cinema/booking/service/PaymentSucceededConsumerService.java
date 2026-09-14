package com.cinema.booking.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentSucceededConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        CONFIRMED
    }

    record Result(Status status) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE);
        }

        public static Result confirmed() {

            return new Result(Status.CONFIRMED);
        }
    }
}
