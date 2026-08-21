package com.cinema.common.outbox.acknowledgement;

import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.retry.OutboxRetryPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DefaultOutboxAcknowledgementService implements OutboxAcknowledgementService {

    private static final int MAXIMUM_ERROR_LENGTH = 2000;

    private final OutboxRepository repository;

    private final OutboxRetryPolicy retryPolicy;

    private final Clock clock;

    public DefaultOutboxAcknowledgementService(
            OutboxRepository repository, OutboxRetryPolicy retryPolicy, Clock clock) {

        this.repository = repository;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean acknowledgeSuccess(UUID eventId, String processingOwner) {

        OffsetDateTime publishedAt = OffsetDateTime.now(clock);

        int updatedRows =
                repository.markSentIfOwned(
                        eventId,
                        processingOwner,
                        publishedAt,
                        OutboxStatus.PROCESSING,
                        OutboxStatus.SENT);

        return updatedRows == 1;
    }

    @Override
    @Transactional
    public boolean acknowledgeFailure(
            UUID eventId, String processingOwner, int currentRetryCount, Throwable exception) {

        OffsetDateTime failedAt = OffsetDateTime.now(clock);

        OffsetDateTime nextAttemptAt = retryPolicy.nextAttemptAt(currentRetryCount, failedAt);

        int updatedRows =
                repository.markFailedIfOwned(
                        eventId,
                        processingOwner,
                        nextAttemptAt,
                        errorMessage(exception),
                        OutboxStatus.PROCESSING,
                        OutboxStatus.FAILED);

        return updatedRows == 1;
    }

    private String errorMessage(Throwable exception) {

        Throwable cause = unwrap(exception);

        String message = cause.getMessage();

        String normalized =
                message == null || message.isBlank()
                        ? cause.getClass().getSimpleName()
                        : message.trim();

        return normalized.length() <= MAXIMUM_ERROR_LENGTH
                ? normalized
                : normalized.substring(0, MAXIMUM_ERROR_LENGTH);
    }

    private Throwable unwrap(Throwable exception) {

        Throwable current = exception;

        while (current.getCause() != null && current.getCause() != current) {

            current = current.getCause();
        }

        return current;
    }
}
