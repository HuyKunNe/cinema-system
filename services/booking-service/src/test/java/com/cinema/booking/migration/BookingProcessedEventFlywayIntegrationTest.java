package com.cinema.booking.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

class BookingProcessedEventFlywayIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void migrationShouldCreateProcessedEventsTable() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = 'processed_events'
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void processedEventIdentifiersShouldUseBinarySixteen() {

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name,
                               data_type,
                               character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'processed_events'
                          AND column_name IN (
                              'id',
                              'event_id'
                          )
                        """);

        assertThat(columns).hasSize(2);

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
    void migrationShouldCreateProcessedEventIndexes() {

        List<String> indexes =
                jdbcTemplate.queryForList(
                        """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'processed_events'
                          AND index_name IN (
                              'idx_processed_events_processed_at',
                              'idx_processed_events_type'
                          )
                        """,
                        String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_processed_events_processed_at", "idx_processed_events_type");
    }

    @Test
    void processedEventsShouldHaveNoForeignKeys() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name = 'processed_events'
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                        Integer.class);

        assertThat(count).isZero();
    }

    @Test
    void flywayShouldContainSuccessfulVersionFive() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '5'
                          AND success = TRUE
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
