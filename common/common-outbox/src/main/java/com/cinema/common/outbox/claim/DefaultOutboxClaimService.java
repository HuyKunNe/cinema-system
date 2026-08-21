package com.cinema.common.outbox.claim;

import com.cinema.common.outbox.config.OutboxProperties;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DefaultOutboxClaimService implements OutboxClaimService {

    private static final String CLAIM_OWNER_PREFIX = "outbox-claim:";

    private final OutboxRepository repository;

    private final OutboxProperties properties;

    private final Clock clock;

    public DefaultOutboxClaimService(
            OutboxRepository repository, OutboxProperties properties, Clock clock) {

        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<OutboxEventEntity> claimNextBatch() {

        OffsetDateTime claimedAt = OffsetDateTime.now(clock);

        OffsetDateTime leaseExpiresAt = claimedAt.plus(properties.leaseDuration());

        List<OutboxEventEntity> events =
                repository.findClaimableEvents(
                        claimedAt, properties.maximumAttempts(), properties.batchSize());

        if (events.isEmpty()) {
            return List.of();
        }

        events.forEach(event -> event.claim(newClaimOwner(), claimedAt, leaseExpiresAt));

        repository.saveAll(events);

        return List.copyOf(events);
    }

    private String newClaimOwner() {

        return CLAIM_OWNER_PREFIX + UUID.randomUUID();
    }
}
