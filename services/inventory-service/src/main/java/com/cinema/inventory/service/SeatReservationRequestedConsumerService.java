package com.cinema.inventory.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

import java.util.UUID;

public interface SeatReservationRequestedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        RESERVED,

        REJECTED
    }

    record Result(Status status, UUID outboxEventId) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE, null);
        }

        public static Result reserved(UUID outboxEventId) {

            return new Result(Status.RESERVED, outboxEventId);
        }

        public static Result rejected(UUID outboxEventId) {

            return new Result(Status.REJECTED, outboxEventId);
        }
    }
}
