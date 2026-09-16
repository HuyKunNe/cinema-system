package com.cinema.inventory.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface BookingExpiredConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,
        RELEASED
    }

    record Result(Status status) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE);
        }

        public static Result released() {

            return new Result(Status.RELEASED);
        }
    }
}
