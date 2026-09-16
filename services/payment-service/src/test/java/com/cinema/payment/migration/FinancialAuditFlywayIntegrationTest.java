package com.cinema.payment.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

class FinancialAuditFlywayIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private Flyway flyway;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldApplyFinancialAuditMigration() {

        assertThat(flyway.info().pending()).isEmpty();

        assertThat(flyway.info().applied())
                .anySatisfy(
                        migration ->
                                assertThat(migration.getVersion().getVersion()).isEqualTo("6"));
    }

    @Test
    void migrationShouldCreateFinancialAuditTable() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = 'financial_audit_records'
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void financialAuditUuidColumnsShouldUseBinarySixteen() {

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name,
                               data_type,
                               character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'financial_audit_records'
                          AND column_name IN (
                              'id',
                              'payment_id',
                              'correlation_id'
                          )
                        """);

        assertThat(columns).hasSize(3);

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
    void migrationShouldCreateFinancialAuditForeignKey() {

        List<String> constraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name = 'financial_audit_records'
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                        String.class);

        assertThat(constraints).containsExactly("fk_financial_audit_records_payment");
    }

    @Test
    void migrationShouldCreateFinancialAuditIndexes() {

        List<String> indexes =
                jdbcTemplate.queryForList(
                        """
                        SELECT DISTINCT index_name
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'financial_audit_records'
                          AND index_name IN (
                              'idx_financial_audit_payment_occurred',
                              'idx_financial_audit_actor_occurred',
                              'idx_financial_audit_action_occurred'
                          )
                        """,
                        String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_financial_audit_payment_occurred",
                        "idx_financial_audit_actor_occurred",
                        "idx_financial_audit_action_occurred");
    }
}
