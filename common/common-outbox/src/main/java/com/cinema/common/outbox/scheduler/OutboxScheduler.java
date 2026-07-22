package com.cinema.common.outbox.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.publisher.OutboxPublisher;
import com.cinema.common.outbox.repository.OutboxRepository;

@Component
@EnableScheduling
public class OutboxScheduler {

    private final OutboxRepository repository;

    private final OutboxPublisher publisher;

    public OutboxScheduler(
            OutboxRepository repository,
            OutboxPublisher publisher) {

        this.repository = repository;

        this.publisher = publisher;

    }

    @Scheduled(fixedDelayString = "${cinema.outbox.delay:5000}")
    public void execute() {

        repository
                .findTop100ByStatusInOrderByCreatedAt(
                        List.of(
                                OutboxStatus.PENDING,
                                OutboxStatus.FAILED))
                .stream()

                .filter(
                        OutboxEventEntity::canRetry)

                .forEach(
                        this::publish);

    }

    private void publish(
            OutboxEventEntity event) {

        event.markProcessing();

        repository.save(event);

        publisher.publish(event)

                .thenRun(() -> {

                    event.markSent();

                    repository.save(event);

                })

                .exceptionally(
                        exception -> {

                            event.markFailed();

                            repository.save(event);

                            return null;

                        });

    }

}
