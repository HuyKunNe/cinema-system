package com.cinema.inventory.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

import java.util.UUID;

public interface SeatReleaseRequestedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        DUPLICATE,

        RELEASED
    }

    record Result(Status status, UUID outboxEventId) {

        public static Result alreadyProcessed() {

            return new Result(Status.DUPLICATE, null);
        }

        public static Result released(UUID outboxEventId) {

            return new Result(Status.RELEASED, outboxEventId);
        }
    }
}
