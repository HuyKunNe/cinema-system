package com.cinema.booking.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

class BookingFlywayIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of("bookings", "booking_seats");

    @Autowired private Flyway flyway;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldApplyAllMigrationsSuccessfully() {
        MigrationInfoService migrationInfo = flyway.info();

        assertThat(migrationInfo.pending()).isEmpty();

        assertThat(migrationInfo.all()).filteredOn(info -> info.getState().isFailed()).isEmpty();

        assertThat(migrationInfo.applied()).isNotEmpty();
    }

    @Test
    void flywaySchemaHistoryShouldContainSuccessfulVersionOne() {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '1'
                          AND success = TRUE
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void migrationShouldCreateBookingTables() {
        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'bookings',
                              'booking_seats'
                          )
                        ORDER BY table_name
                        """,
                        String.class);

        assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
    }

    @Test
    void uuidColumnsShouldUseBinarySixteen() {
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
                                  table_name = 'bookings'
                                  AND column_name IN (
                                      'id',
                                      'user_id',
                                      'showtime_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'booking_seats'
                                  AND column_name IN (
                                      'id',
                                      'booking_id',
                                      'inventory_seat_id',
                                      'showtime_id'
                                  )
                              )
                          )
                        """);

        assertThat(columns).hasSize(7);

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
    void pendingMoneyFieldsShouldBeNullable() {
        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name,
                               is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'bookings'
                          AND column_name IN (
                              'total_amount',
                              'currency'
                          )
                        """);

        assertThat(columns).hasSize(2);

        assertThat(columns)
                .allSatisfy(
                        column ->
                                assertThat(column.get("is_nullable"))
                                        .asString()
                                        .isEqualToIgnoringCase("YES"));
    }

    @Test
    void pendingSeatSnapshotFieldsShouldBeNullable() {
        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name,
                               is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'booking_seats'
                          AND column_name IN (
                              'inventory_seat_id',
                              'seat_type',
                              'price'
                          )
                        """);

        assertThat(columns).hasSize(3);

        assertThat(columns)
                .allSatisfy(
                        column ->
                                assertThat(column.get("is_nullable"))
                                        .asString()
                                        .isEqualToIgnoringCase("YES"));
    }

    @Test
    void migrationShouldCreateExpectedUniqueConstraints() {
        List<String> constraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name IN (
                              'uk_bookings_user_client_request',
                              'uk_booking_seats_booking_seat'
                          )
                        """,
                        String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "uk_bookings_user_client_request", "uk_booking_seats_booking_seat");
    }

    @Test
    void migrationShouldCreateOnlyInternalBookingForeignKey() {
        List<String> constraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                        String.class);

        assertThat(constraints).containsExactly("fk_booking_seats_booking");
    }

    @Test
    void migrationShouldCreateCriticalIndexes() {
        List<String> indexes =
                jdbcTemplate.queryForList(
                        """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name IN (
                              'idx_bookings_user_created',
                              'idx_bookings_status_expiration',
                              'idx_booking_seats_booking'
                          )
                        """,
                        String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_bookings_user_created",
                        "idx_bookings_status_expiration",
                        "idx_booking_seats_booking");
    }
}
