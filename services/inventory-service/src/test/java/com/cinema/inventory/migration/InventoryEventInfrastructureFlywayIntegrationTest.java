package com.cinema.inventory.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

class InventoryEventInfrastructureFlywayIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void migrationShouldCreateEventInfrastructureTables() {

        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'processed_events',
                              'outbox_events'
                          )
                        """,
                        String.class);

        assertThat(tables).containsExactlyInAnyOrder("processed_events", "outbox_events");
    }

    @Test
    void eventIdentifiersShouldUseBinarySixteen() {

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name,
                               column_name,
                               data_type,
                               character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND (
                              (
                                  table_name = 'processed_events'
                                  AND column_name IN (
                                      'id',
                                      'event_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'outbox_events'
                                  AND column_name IN (
                                      'id',
                                      'aggregate_id',
                                      'correlation_id',
                                      'causation_id'
                                  )
                              )
                          )
                        """);

        assertThat(columns).hasSize(6);

        assertThat(columns)
                .allSatisfy(
                        column -> {
                            assertThat(column.get("data_type"))
                                    .asString()
                                    .isEqualToIgnoringCase("binary");

                            assertThat(
                                            ((Number) column.get("character_maximum_length"))
                                                    .longValue())
                                    .isEqualTo(16L);
                        });
    }

    @Test
    void processedEventShouldHaveConsumerIdempotencyConstraint() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name = 'processed_events'
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name =
                              'uk_processed_events_event_consumer'
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void outboxShouldContainHardenedClaimColumns() {

        List<String> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'outbox_events'
                          AND column_name IN (
                              'next_attempt_at',
                              'last_error',
                              'processing_owner',
                              'processing_started_at',
                              'processing_expires_at'
                          )
                        """,
                        String.class);

        assertThat(columns)
                .containsExactlyInAnyOrder(
                        "next_attempt_at",
                        "last_error",
                        "processing_owner",
                        "processing_started_at",
                        "processing_expires_at");
    }

    @Test
    void migrationShouldCreateCriticalEventIndexes() {

        List<String> indexes =
                jdbcTemplate.queryForList(
                        """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name IN (
                              'idx_processed_events_processed_at',
                              'idx_processed_events_type',
                              'idx_outbox_events_claim',
                              'idx_outbox_events_processing_owner',
                              'idx_outbox_events_aggregate',
                              'idx_outbox_events_correlation'
                          )
                        """,
                        String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_processed_events_processed_at",
                        "idx_processed_events_type",
                        "idx_outbox_events_claim",
                        "idx_outbox_events_processing_owner",
                        "idx_outbox_events_aggregate",
                        "idx_outbox_events_correlation");
    }

    @Test
    void eventInfrastructureShouldHaveNoForeignKeys() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name IN (
                              'processed_events',
                              'outbox_events'
                          )
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                        Integer.class);

        assertThat(count).isZero();
    }
}
