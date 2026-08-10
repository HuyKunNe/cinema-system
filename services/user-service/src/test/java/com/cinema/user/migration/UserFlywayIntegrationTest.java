package com.cinema.user.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            "user_credentials",
            "roles",
            "permissions",
            "user_roles",
            "email_verification_tokens",
            "role_permissions",
            "oauth2_registered_client");

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
    void flywaySchemaHistoryShouldContainSuccessfulMigrations() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version IN ('1', '2', '3', '4')
                          AND success = TRUE
                        """,
                Integer.class);

        assertThat(count).isEqualTo(4);
    }

    @Test
    void migrationShouldCreateAllUserServiceTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'users',
                              'user_profiles',
                              'user_credentials',
                              'roles',
                              'permissions',
                              'user_roles',
                              'role_permissions',
                              'email_verification_tokens',
                              'oauth2_registered_client'
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
                              (
                                  table_name = 'users'
                                  AND column_name = 'id'
                              )
                              OR
                              (
                                  table_name = 'user_profiles'
                                  AND column_name IN (
                                      'id',
                                      'user_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'user_credentials'
                                  AND column_name IN (
                                      'id',
                                      'user_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'roles'
                                  AND column_name = 'id'
                              )
                              OR
                              (
                                  table_name = 'permissions'
                                  AND column_name = 'id'
                              )
                              OR
                              (
                                  table_name = 'user_roles'
                                  AND column_name IN (
                                      'user_id',
                                      'role_id',
                                      'assigned_by_user_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'role_permissions'
                                  AND column_name IN (
                                      'role_id',
                                      'permission_id',
                                      'assigned_by_user_id'
                                  )
                              )
                                OR
                                (
                                    table_name = 'email_verification_tokens'
                                    AND column_name IN (
                                        'id',
                                        'user_id'
                                    )
                                )
                          )
                        """);

        assertThat(columns).hasSize(15);

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
                              'uk_user_credentials_user',
                              'uk_roles_name',
                              'uk_permissions_code',
                              'uk_email_verification_tokens_hash'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "uk_users_normalized_email",
                        "uk_users_normalized_username",
                        "uk_user_profiles_user",
                        "uk_user_credentials_user",
                        "uk_roles_name",
                        "uk_permissions_code",
                        "uk_email_verification_tokens_hash");
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
                              'fk_user_credentials_user',
                              'fk_user_roles_user',
                              'fk_user_roles_role',
                              'fk_user_roles_assigned_by',
                              'fk_role_permissions_role',
                              'fk_role_permissions_permission',
                              'fk_role_permissions_assigned_by',
                              'fk_email_verification_tokens_user'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "fk_user_profiles_user",
                        "fk_user_credentials_user",
                        "fk_user_roles_user",
                        "fk_user_roles_role",
                        "fk_user_roles_assigned_by",
                        "fk_role_permissions_role",
                        "fk_role_permissions_permission",
                        "fk_role_permissions_assigned_by",
                        "fk_email_verification_tokens_user");
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
                              'chk_user_credentials_failed_attempts',
                              'chk_roles_name',
                              'chk_email_verification_token_hash',
                              'chk_email_verification_token_expiration',
                              'chk_email_verification_token_terminal_state'
                          )
                        """,
                String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "chk_users_status",
                        "chk_user_credentials_failed_attempts",
                        "chk_roles_name",
                        "chk_email_verification_token_hash",
                        "chk_email_verification_token_expiration",
                        "chk_email_verification_token_terminal_state");
    }

    @Test
    void migrationShouldCreateCriticalIndexes() {
        List<String> indexes = jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND index_name IN (
                              'idx_users_status',
                              'idx_user_roles_role',
                              'idx_user_roles_assigned_by',
                              'idx_role_permissions_permission',
                              'idx_role_permissions_assigned_by',
                              'idx_email_verification_tokens_user_active',
                              'idx_email_verification_tokens_expires_at'
                          )
                        """,
                String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_users_status",
                        "idx_user_roles_role",
                        "idx_user_roles_assigned_by",
                        "idx_role_permissions_permission",
                        "idx_role_permissions_assigned_by",
                        "idx_email_verification_tokens_user_active",
                        "idx_email_verification_tokens_expires_at");
    }

    @Test
    void migrationShouldSeedAuthorityCatalog() {
        Integer roleCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM roles
                        """,
                Integer.class);

        Integer permissionCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM permissions
                        """,
                Integer.class);

        Integer assignmentCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM role_permissions
                        """,
                Integer.class);

        assertThat(roleCount).isEqualTo(4);
        assertThat(permissionCount).isEqualTo(9);
        assertThat(assignmentCount).isEqualTo(19);
    }

    @Test
    void migrationShouldSeedExpectedRolePermissionCounts() {
        assertThat(permissionCountForRole("USER"))
                .isEqualTo(3);

        assertThat(permissionCountForRole("STAFF"))
                .isEqualTo(7);

        assertThat(permissionCountForRole("ADMIN"))
                .isEqualTo(9);

        assertThat(permissionCountForRole("SERVICE"))
                .isZero();
    }

    @Test
    void seededAuthorityIdsShouldUseUuidVersionSeven() {
        List<String> identifiers = jdbcTemplate.queryForList(
                """
                        SELECT BIN_TO_UUID(id)
                        FROM roles

                        UNION ALL

                        SELECT BIN_TO_UUID(id)
                        FROM permissions
                        """,
                String.class);

        assertThat(identifiers).hasSize(13);

        assertThat(identifiers)
                .allSatisfy(identifier -> assertThat(
                        UUID.fromString(identifier).version())
                        .isEqualTo(7));
    }

    @Test
    void hibernateShouldValidateFlywaySchema() {
        assertThat(entityManagerFactory.isOpen())
                .isTrue();
    }

    @Test
    void migrationShouldCreateRegisteredClientUniqueConstraint() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name =
                              'oauth2_registered_client'
                          AND constraint_type = 'UNIQUE'
                        """,
                String.class);

        assertThat(constraints)
                .contains(
                        "uk_oauth2_registered_client_client_id");
    }

    @Test
    void registeredClientTableShouldContainExpectedColumns() {
        List<String> columns = jdbcTemplate.queryForList(
                """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name =
                              'oauth2_registered_client'
                        """,
                String.class);

        assertThat(columns)
                .containsExactlyInAnyOrder(
                        "id",
                        "client_id",
                        "client_id_issued_at",
                        "client_secret",
                        "client_secret_expires_at",
                        "client_name",
                        "client_authentication_methods",
                        "authorization_grant_types",
                        "redirect_uris",
                        "post_logout_redirect_uris",
                        "scopes",
                        "client_settings",
                        "token_settings");
    }

    private int permissionCountForRole(String roleName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(role_permissions.permission_id)
                        FROM roles
                        LEFT JOIN role_permissions
                            ON role_permissions.role_id = roles.id
                        WHERE roles.name = ?
                        """,
                Integer.class,
                roleName);

        return count == null ? 0 : count;
    }
}
