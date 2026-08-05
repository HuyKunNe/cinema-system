package com.cinema.user.migration;

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

import jakarta.persistence.EntityManagerFactory;

class UserFlywayIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "user_profiles",
            "user_credentials");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    void migrationShouldCreateUserTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'users',
                              'user_profiles',
                              'user_credentials'
                          )
                        ORDER BY table_name
                        """,
                String.class);

        assertThat(tables)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
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
                              (table_name = 'users'
                                  AND column_name = 'id')
                              OR
                              (table_name = 'user_profiles'
                                  AND column_name IN (
                                      'id',
                                      'user_id'
                                  ))
                              OR
                              (table_name = 'user_credentials'
                                  AND column_name IN (
                                      'id',
                                      'user_id'
                                  ))
                          )
                        """);

        assertThat(columns).hasSize(5);

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
    void migrationShouldCreateExpectedUniqueConstraints() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name IN (
                              'uk_users_normalized_email',
                              'uk_users_normalized_username',
                              'uk_user_profiles_user',
                              'uk_user_credentials_user'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "uk_users_normalized_email",
                        "uk_users_normalized_username",
                        "uk_user_profiles_user",
                        "uk_user_credentials_user");
    }

    @Test
    void migrationShouldCreateExpectedForeignKeys() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'FOREIGN KEY'
                          AND constraint_name IN (
                              'fk_user_profiles_user',
                              'fk_user_credentials_user'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "fk_user_profiles_user",
                        "fk_user_credentials_user");
    }

    @Test
    void migrationShouldCreateExpectedChecks() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'CHECK'
                          AND constraint_name IN (
                              'chk_users_status',
                              'chk_user_credentials_failed_attempts'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "chk_users_status",
                        "chk_user_credentials_failed_attempts");
    }

    @Test
    void migrationShouldCreateStatusIndex() {
        List<String> indexes = jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name = 'idx_users_status'
                        """,
                String.class);

        assertThat(indexes)
                .containsExactly("idx_users_status");
    }

    @Test
    void hibernateShouldValidateFlywaySchema() {
        assertThat(entityManagerFactory.isOpen())
                .isTrue();
    }
}
