package com.cinema.common.outbox.scheduler;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.publisher.OutboxPublisher;
import com.cinema.common.outbox.repository.OutboxRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxScheduler {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository repository;

    private final OutboxPublisher publisher;

    private final Clock clock;

    public OutboxScheduler(OutboxRepository repository, OutboxPublisher publisher, Clock clock) {

        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${cinema.outbox.delay:5000}")
    public void publishPendingEvents() {

        List<OutboxEventEntity> events =
                repository.findByStatusInOrderByCreatedAtAsc(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                        PageRequest.of(0, BATCH_SIZE));

        events.stream().filter(OutboxEventEntity::canRetry).forEach(this::publish);
    }

    private void publish(OutboxEventEntity event) {

        event.markProcessing();

        repository.save(event);

        publisher
                .publish(event)
                .whenComplete(
                        (ignored, exception) -> {
                            if (exception == null) {
                                event.markSent(OffsetDateTime.now(clock));
                            } else {
                                event.markFailed();
                            }

                            repository.save(event);
                        });
    }
}
