package com.cinema.booking.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservationRejectedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        REJECTED
    }

    record Result(Status status) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE);
        }

        public static Result rejected() {

            return new Result(Status.REJECTED);
        }
    }
}
