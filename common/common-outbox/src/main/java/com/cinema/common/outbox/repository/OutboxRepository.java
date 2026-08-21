package com.cinema.common.outbox.repository;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value =
                    """
                    SELECT *
                    FROM outbox_events
                    WHERE
                        (
                            status IN ('PENDING', 'FAILED')
                            AND retry_count < :maximumAttempts
                            AND (
                                next_attempt_at IS NULL
                                OR next_attempt_at <= :now
                            )
                        )
                        OR
                        (
                            status = 'PROCESSING'
                            AND retry_count < :maximumAttempts
                            AND processing_expires_at IS NOT NULL
                            AND processing_expires_at <= :now
                        )
                    ORDER BY created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEventEntity> findClaimableEvents(
            @Param("now") OffsetDateTime now,
            @Param("maximumAttempts") int maximumAttempts,
            @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE OutboxEventEntity event
            SET event.status = :sentStatus,
                event.publishedAt = :publishedAt,
                event.nextAttemptAt = NULL,
                event.lastError = NULL,
                event.processingOwner = NULL,
                event.processingStartedAt = NULL,
                event.processingExpiresAt = NULL
            WHERE event.id = :eventId
              AND event.status = :processingStatus
              AND event.processingOwner = :processingOwner
            """)
    int markSentIfOwned(
            @Param("eventId") UUID eventId,
            @Param("processingOwner") String processingOwner,
            @Param("publishedAt") OffsetDateTime publishedAt,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("sentStatus") OutboxStatus sentStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE OutboxEventEntity event
            SET event.status = :failedStatus,
                event.retryCount = event.retryCount + 1,
                event.nextAttemptAt = :nextAttemptAt,
                event.lastError = :lastError,
                event.processingOwner = NULL,
                event.processingStartedAt = NULL,
                event.processingExpiresAt = NULL
            WHERE event.id = :eventId
              AND event.status = :processingStatus
              AND event.processingOwner = :processingOwner
            """)
    int markFailedIfOwned(
            @Param("eventId") UUID eventId,
            @Param("processingOwner") String processingOwner,
            @Param("nextAttemptAt") OffsetDateTime nextAttemptAt,
            @Param("lastError") String lastError,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("failedStatus") OutboxStatus failedStatus);
}
