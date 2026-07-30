package com.cinema.inventory.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

class InventoryFlywayIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "cinemas",
            "rooms",
            "seats",
            "showtimes",
            "show_seats");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldApplyAllMigrationsSuccessfully() {
        MigrationInfoService migrationInfo = flyway.info();

        assertThat(migrationInfo.pending())
                .isEmpty();

        assertThat(migrationInfo.all())
                .filteredOn(info -> info.getState().isFailed())
                .isEmpty();

        assertThat(migrationInfo.applied())
                .isNotEmpty();

        assertThat(migrationInfo.applied())
                .extracting(MigrationInfo::getVersion)
                .allMatch(version -> version != null);
    }

    @Test
    void flywaySchemaHistoryShouldContainSuccessfulVersionOne() {
        Integer count = jdbcTemplate.queryForObject(
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
    void migrationShouldCreateAllInventoryTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'cinemas',
                              'rooms',
                              'seats',
                              'showtimes',
                              'show_seats'
                          )
                        ORDER BY table_name
                        """,
                String.class);

        assertThat(tables)
                .containsExactlyInAnyOrderElementsOf(
                        EXPECTED_TABLES);
    }

    @Test
    void uuidColumnsShouldUseBinarySixteen() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                """
                        SELECT table_name,
                               column_name,
                               data_type,
                               character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND (
                              (table_name = 'cinemas'
                                  AND column_name = 'id')
                              OR
                              (table_name = 'rooms'
                                  AND column_name IN (
                                      'id',
                                      'cinema_id'
                                  ))
                              OR
                              (table_name = 'seats'
                                  AND column_name IN (
                                      'id',
                                      'room_id'
                                  ))
                              OR
                              (table_name = 'showtimes'
                                  AND column_name IN (
                                      'id',
                                      'movie_id',
                                      'room_id'
                                  ))
                              OR
                              (table_name = 'show_seats'
                                  AND column_name IN (
                                      'id',
                                      'showtime_id',
                                      'seat_id',
                                      'held_by_booking_id'
                                  ))
                          )
                        """);

        assertThat(columns).hasSize(12);

        assertThat(columns)
                .allSatisfy(column -> {
                    assertThat(column.get("data_type"))
                            .asString()
                            .isEqualToIgnoringCase("binary");

                    assertThat(
                            ((Number) column.get(
                                    "character_maximum_length"))
                                    .longValue())
                            .isEqualTo(16L);
                });
    }

    @Test
    void showSeatPriceShouldUseExpectedDecimalDefinition() {
        Map<String, Object> priceColumn = jdbcTemplate.queryForMap(
                """
                        SELECT data_type,
                               numeric_precision,
                               numeric_scale,
                               is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'show_seats'
                          AND column_name = 'price'
                        """);

        assertThat(priceColumn.get("data_type"))
                .asString()
                .isEqualToIgnoringCase("decimal");

        assertThat(
                ((Number) priceColumn.get(
                        "numeric_precision"))
                        .intValue())
                .isEqualTo(12);

        assertThat(
                ((Number) priceColumn.get(
                        "numeric_scale"))
                        .intValue())
                .isEqualTo(2);

        assertThat(priceColumn.get("is_nullable"))
                .asString()
                .isEqualToIgnoringCase("NO");
    }

    @Test
    void migrationShouldCreateExpectedUniqueConstraints() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name IN (
                              'uk_rooms_cinema_name',
                              'uk_seats_room_number',
                              'uk_showtimes_room_start',
                              'uk_show_seats_showtime_seat',
                              'uk_show_seats_showtime_number'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "uk_rooms_cinema_name",
                        "uk_seats_room_number",
                        "uk_showtimes_room_start",
                        "uk_show_seats_showtime_seat",
                        "uk_show_seats_showtime_number");
    }

    @Test
    void migrationShouldCreateExpectedForeignKeys() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type =
                              'FOREIGN KEY'
                          AND constraint_name IN (
                              'fk_rooms_cinema',
                              'fk_seats_room',
                              'fk_showtimes_room',
                              'fk_show_seats_showtime',
                              'fk_show_seats_seat'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "fk_rooms_cinema",
                        "fk_seats_room",
                        "fk_showtimes_room",
                        "fk_show_seats_showtime",
                        "fk_show_seats_seat");
    }

    @Test
    void migrationShouldCreateCriticalIndexes() {
        List<String> indexes = jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name IN (
                              'idx_cinemas_city_active',
                              'idx_rooms_cinema_active',
                              'idx_seats_room_active',
                              'idx_showtimes_movie_start',
                              'idx_showtimes_room_status_start',
                              'idx_show_seats_showtime_status',
                              'idx_show_seats_booking',
                              'idx_show_seats_expired_hold'
                          )
                        """,
                String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_cinemas_city_active",
                        "idx_rooms_cinema_active",
                        "idx_seats_room_active",
                        "idx_showtimes_movie_start",
                        "idx_showtimes_room_status_start",
                        "idx_show_seats_showtime_status",
                        "idx_show_seats_booking",
                        "idx_show_seats_expired_hold");
    }
}
