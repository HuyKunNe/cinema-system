package com.cinema.payment.service;

import com.cinema.common.outbox.model.OutboxEventMessage;

import java.util.UUID;

public interface PaymentRequestedConsumerService {

    Result handle(String partitionKey, OutboxEventMessage message);

    enum Status {
        CREATED,
        EXPIRED,
        EXISTING,
        ALREADY_PROCESSED
    }

    record Result(Status status, UUID paymentId) {

        public static Result created(UUID paymentId) {

            return new Result(Status.CREATED, paymentId);
        }

        public static Result expired(UUID paymentId) {

            return new Result(Status.EXPIRED, paymentId);
        }

        public static Result existing(UUID paymentId) {

            return new Result(Status.EXISTING, paymentId);
        }

        public static Result alreadyProcessed() {

            return new Result(Status.ALREADY_PROCESSED, null);
        }
    }
}
