package com.cinema.common.outbox.scheduler;

import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.publisher.OutboxPublisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@ConditionalOnProperty(
        prefix = "cinema.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxClaimService claimService;

    private final OutboxPublisher publisher;

    private final OutboxAcknowledgementService acknowledgementService;

    public OutboxScheduler(
            OutboxClaimService claimService,
            OutboxPublisher publisher,
            OutboxAcknowledgementService acknowledgementService) {

        this.claimService = claimService;
        this.publisher = publisher;
        this.acknowledgementService = acknowledgementService;
    }

    @Scheduled(fixedDelayString = "${cinema.outbox.scheduler-delay:5s}")
    public void publishPendingEvents() {

        claimService.claimNextBatch().forEach(this::publish);
    }

    private void publish(OutboxEventEntity event) {

        String processingOwner = event.getProcessingOwner();

        int currentRetryCount = event.getRetryCount();

        publisher
                .publish(event)
                .whenComplete(
                        (ignored, exception) -> {
                            if (exception == null) {
                                acknowledgementService.acknowledgeSuccess(
                                        event.getId(), processingOwner);

                            } else {
                                acknowledgementService.acknowledgeFailure(
                                        event.getId(),
                                        processingOwner,
                                        currentRetryCount,
                                        exception);
                            }
                        });
    }
}
