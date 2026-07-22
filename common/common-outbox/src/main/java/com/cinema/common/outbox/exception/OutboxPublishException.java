package com.cinema.common.outbox.exception;

public class OutboxPublishException
        extends RuntimeException {

    public OutboxPublishException(
            Throwable cause) {

        super(
                "Failed to publish outbox event",
                cause);

    }

}
