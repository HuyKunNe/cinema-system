package com.cinema.common.outbox.publisher;

import java.util.concurrent.CompletableFuture;

import com.cinema.common.outbox.entity.OutboxEventEntity;

public interface OutboxPublisher {

    CompletableFuture<Void> publish(OutboxEventEntity event);

}
