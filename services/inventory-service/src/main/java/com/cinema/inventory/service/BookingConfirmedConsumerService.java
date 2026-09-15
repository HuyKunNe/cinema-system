package com.cinema.inventory.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface BookingConfirmedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        BOOKED
    }

    record Result(Status status) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE);
        }

        public static Result booked() {

            return new Result(Status.BOOKED);
        }
    }
}
