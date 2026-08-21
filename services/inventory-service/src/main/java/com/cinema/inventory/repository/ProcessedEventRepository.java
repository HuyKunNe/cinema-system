package com.cinema.inventory.repository;

import com.cinema.inventory.entity.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT IGNORE INTO processed_events (
                        id,
                        event_id,
                        consumer_name,
                        event_type,
                        event_version,
                        processed_at
                    )
                    VALUES (
                        UUID_TO_BIN(:id),
                        UUID_TO_BIN(:eventId),
                        :consumerName,
                        :eventType,
                        :eventVersion,
                        :processedAt
                    )
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("eventId") String eventId,
            @Param("consumerName") String consumerName,
            @Param("eventType") String eventType,
            @Param("eventVersion") String eventVersion,
            @Param("processedAt") OffsetDateTime processedAt);
}
