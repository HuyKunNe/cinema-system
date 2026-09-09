package com.cinema.payment.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

class PaymentFlywayIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final List<String> EXPECTED_TABLES =
            List.of(
                    "outbox_events",
                    "payment_provider_webhook_events",
                    "payment_transactions",
                    "payments",
                    "processed_events");

    @Autowired private Flyway flyway;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void flywayShouldApplyAllMigrationsSuccessfully() {

        MigrationInfoService migrationInfo = flyway.info();

        assertThat(migrationInfo.pending()).isEmpty();

        assertThat(migrationInfo.all()).filteredOn(info -> info.getState().isFailed()).isEmpty();

        assertThat(migrationInfo.applied()).hasSize(5);
    }

    @Test
    void flywayHistoryShouldContainSuccessfulVersions() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version IN (
                            '1',
                            '2',
                            '3',
                            '4',
                            '5'
                        )
                          AND success = TRUE
                        """,
                        Integer.class);

        assertThat(count).isEqualTo(5);
    }

    @Test
    void hibernateShouldValidatePaymentMappings() {

        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    void migrationShouldCreatePaymentOwnedTables() {

        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'payments',
                              'payment_transactions',
                              'processed_events',
                              'payment_provider_webhook_events',
                              'outbox_events'
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
                                  table_name = 'payments'
                                  AND column_name IN (
                                      'id',
                                      'booking_id',
                                      'user_id',
                                      'source_event_id',
                                      'correlation_id'
                                  )
                              )
                              OR
                              (
                                  table_name = 'payment_transactions'
                                  AND column_name IN (
                                      'id',
                                      'payment_id'
                                  )
                              )
                              OR
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
                            OR
                            (
                                table_name = 'payment_provider_webhook_events'
                                AND column_name IN (
                                    'id',
                                    'payment_transaction_id'
                                )
                            )
                          )
                        """);

        assertThat(columns).hasSize(15);

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
    void migrationShouldCreateExpectedUniqueConstraints() {

        List<String> constraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name IN (
                              'uk_payments_booking_attempt',
                              'uk_payments_source_event',
                              'uk_payments_provider_reference',
                              'uk_payment_transactions_provider_idempotency',
                              'uk_payment_transactions_provider_event',
                              'uk_payment_transactions_provider_reference',
                              'uk_processed_events_event_consumer',
                              'uk_payment_provider_webhook_events_provider_event'
                          )
                        """,
                        String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "uk_payments_booking_attempt",
                        "uk_payments_source_event",
                        "uk_payments_provider_reference",
                        "uk_payment_transactions_provider_idempotency",
                        "uk_payment_transactions_provider_event",
                        "uk_payment_transactions_provider_reference",
                        "uk_processed_events_event_consumer",
                        "uk_payment_provider_webhook_events_provider_event");
    }

    @Test
    void migrationShouldCreateOnlyInternalPaymentForeignKey() {

        List<String> constraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                        String.class);

        assertThat(constraints)
                .containsExactlyInAnyOrder(
                        "fk_payment_transactions_payment",
                        "fk_payment_provider_webhook_events_transaction");
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
                              'idx_payments_booking',
                              'idx_payments_user_created',
                              'idx_payments_status_hold_expiration',
                              'idx_payment_transactions_payment',
                              'idx_payment_transactions_claim',
                              'idx_processed_events_processed_at',
                              'idx_processed_events_type',
                              'idx_outbox_events_claim',
                              'idx_outbox_events_processing_owner',
                              'idx_outbox_events_aggregate',
                              'idx_outbox_events_correlation',
                              'idx_payment_transactions_ready_claim',
                              'idx_payment_provider_webhook_events_transaction'
                          )
                        """,
                        String.class);

        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_payments_booking",
                        "idx_payments_user_created",
                        "idx_payments_status_hold_expiration",
                        "idx_payment_transactions_payment",
                        "idx_payment_transactions_claim",
                        "idx_processed_events_processed_at",
                        "idx_processed_events_type",
                        "idx_outbox_events_claim",
                        "idx_outbox_events_processing_owner",
                        "idx_outbox_events_aggregate",
                        "idx_outbox_events_correlation",
                        "idx_payment_transactions_ready_claim",
                        "idx_payment_provider_webhook_events_transaction");
    }

    @Test
    void moneyColumnsShouldUseDecimalNineteenTwo() {

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name,
                               column_name,
                               numeric_precision,
                               numeric_scale,
                               is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND column_name = 'amount'
                          AND table_name IN (
                              'payments',
                              'payment_transactions'
                          )
                        """);

        assertThat(columns).hasSize(2);

        assertThat(columns)
                .allSatisfy(
                        column -> {
                            assertThat(((Number) column.get("numeric_precision")).intValue())
                                    .isEqualTo(19);

                            assertThat(((Number) column.get("numeric_scale")).intValue())
                                    .isEqualTo(2);

                            assertThat(column.get("is_nullable"))
                                    .asString()
                                    .isEqualToIgnoringCase("NO");
                        });
    }

    @Test
    void databaseShouldRejectInvalidPaymentStatus() {

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO payments (
                                            id,
                                            booking_id,
                                            user_id,
                                            payment_attempt,
                                            amount,
                                            currency,
                                            provider,
                                            status,
                                            refund_status,
                                            hold_expires_at,
                                            requested_at,
                                            source_event_id,
                                            correlation_id,
                                            version,
                                            created_at,
                                            updated_at
                                        )
                                        VALUES (
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            1,
                                            250000.00,
                                            'VND',
                                            'MOCK',
                                            'INVALID_STATUS',
                                            'NOT_REQUESTED',
                                            DATE_ADD(
                                                UTC_TIMESTAMP(6),
                                                INTERVAL 10 MINUTE
                                            ),
                                            UTC_TIMESTAMP(6),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            0,
                                            UTC_TIMESTAMP(6),
                                            UTC_TIMESTAMP(6)
                                        )
                                        """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseShouldRejectNegativePaymentAmount() {

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO payments (
                                            id,
                                            booking_id,
                                            user_id,
                                            payment_attempt,
                                            amount,
                                            currency,
                                            provider,
                                            status,
                                            refund_status,
                                            hold_expires_at,
                                            requested_at,
                                            source_event_id,
                                            correlation_id,
                                            version,
                                            created_at,
                                            updated_at
                                        )
                                        VALUES (
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            1,
                                            -0.01,
                                            'VND',
                                            'MOCK',
                                            'RECEIVED',
                                            'NOT_REQUESTED',
                                            DATE_ADD(
                                                UTC_TIMESTAMP(6),
                                                INTERVAL 10 MINUTE
                                            ),
                                            UTC_TIMESTAMP(6),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            0,
                                            UTC_TIMESTAMP(6),
                                            UTC_TIMESTAMP(6)
                                        )
                                        """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseShouldRejectNonCanonicalCurrency() {

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO payments (
                                            id,
                                            booking_id,
                                            user_id,
                                            payment_attempt,
                                            amount,
                                            currency,
                                            provider,
                                            status,
                                            refund_status,
                                            hold_expires_at,
                                            requested_at,
                                            source_event_id,
                                            correlation_id,
                                            version,
                                            created_at,
                                            updated_at
                                        )
                                        VALUES (
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            1,
                                            250000.00,
                                            'vnd',
                                            'MOCK',
                                            'RECEIVED',
                                            'NOT_REQUESTED',
                                            DATE_ADD(
                                                UTC_TIMESTAMP(6),
                                                INTERVAL 10 MINUTE
                                            ),
                                            UTC_TIMESTAMP(6),
                                            UUID_TO_BIN(UUID()),
                                            UUID_TO_BIN(UUID()),
                                            0,
                                            UTC_TIMESTAMP(6),
                                            UTC_TIMESTAMP(6)
                                        )
                                        """))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseShouldRejectProcessingOutboxWithoutLease() {

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO outbox_events (
                                            id,
                                            aggregate_type,
                                            aggregate_id,
                                            event_type,
                                            event_version,
                                            topic,
                                            partition_key,
                                            occurred_at,
                                            payload,
                                            status,
                                            retry_count,
                                            next_attempt_at,
                                            created_at
                                        )
                                        VALUES (
                                            UUID_TO_BIN(UUID()),
                                            'PAYMENT',
                                            UUID_TO_BIN(UUID()),
                                            'payment-succeeded',
                                            '1',
                                            'payment-succeeded',
                                            'booking-1',
                                            UTC_TIMESTAMP(6),
                                            '{}',
                                            'PROCESSING',
                                            0,
                                            UTC_TIMESTAMP(6),
                                            UTC_TIMESTAMP(6)
                                        )
                                        """))
                .isInstanceOf(DataAccessException.class);
    }
}
