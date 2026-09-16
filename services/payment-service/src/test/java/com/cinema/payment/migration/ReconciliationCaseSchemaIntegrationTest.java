package com.cinema.payment.migration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Transactional
class ReconciliationCaseSchemaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void openCaseShouldRejectResolution() {

        UUID paymentId = insertPayment();
        UUID transactionId = insertRefundTransaction(paymentId);

        assertThatThrownBy(
                        () ->
                                insertCase(
                                        paymentId,
                                        transactionId,
                                        "OPEN",
                                        "REFUND_SUCCEEDED",
                                        null,
                                        null,
                                        null))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void resolvedCaseShouldRequireResolution() {

        UUID paymentId = insertPayment();
        UUID transactionId = insertRefundTransaction(paymentId);

        assertThatThrownBy(
                        () ->
                                insertCase(
                                        paymentId,
                                        transactionId,
                                        "RESOLVED",
                                        null,
                                        NOW.plusMinutes(1),
                                        "USER",
                                        "finance-admin"))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void resolvedCaseShouldRequireResolver() {

        UUID paymentId = insertPayment();
        UUID transactionId = insertRefundTransaction(paymentId);

        assertThatThrownBy(
                        () ->
                                insertCase(
                                        paymentId,
                                        transactionId,
                                        "RESOLVED",
                                        "REFUND_SUCCEEDED",
                                        NOW.plusMinutes(1),
                                        null,
                                        null))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void rejectedCaseShouldRejectResolution() {

        UUID paymentId = insertPayment();
        UUID transactionId = insertRefundTransaction(paymentId);

        assertThatThrownBy(
                        () ->
                                insertCase(
                                        paymentId,
                                        transactionId,
                                        "REJECTED",
                                        "REFUND_FAILED",
                                        NOW.plusMinutes(1),
                                        "USER",
                                        "finance-admin"))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void rejectedCaseShouldRequireResolver() {

        UUID paymentId = insertPayment();
        UUID transactionId = insertRefundTransaction(paymentId);

        assertThatThrownBy(
                        () ->
                                insertCase(
                                        paymentId,
                                        transactionId,
                                        "REJECTED",
                                        null,
                                        NOW.plusMinutes(1),
                                        null,
                                        null))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    private UUID insertPayment() {

        UUID paymentId = UuidGenerator.next();

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
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                bytes(paymentId),
                bytes(UuidGenerator.next()),
                bytes(UuidGenerator.next()),
                1,
                "250000.00",
                "VND",
                "MOCK",
                "SUCCEEDED",
                "PENDING",
                Timestamp.from(NOW.plusMinutes(10).toInstant()),
                Timestamp.from(NOW.minusMinutes(10).toInstant()),
                bytes(UuidGenerator.next()),
                bytes(UuidGenerator.next()),
                Timestamp.from(NOW.toInstant()),
                Timestamp.from(NOW.toInstant()));

        return paymentId;
    }

    private UUID insertRefundTransaction(UUID paymentId) {

        UUID transactionId = UuidGenerator.next();

        jdbcTemplate.update(
                """
                INSERT INTO payment_transactions (
                    id,
                    payment_id,
                    provider,
                    transaction_type,
                    attempt_number,
                    status,
                    amount,
                    currency,
                    idempotency_key,
                    requested_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                bytes(transactionId),
                bytes(paymentId),
                "MOCK",
                "REFUND",
                1,
                "PENDING_PROVIDER",
                "250000.00",
                "VND",
                "refund:" + paymentId,
                Timestamp.from(NOW.minusMinutes(1).toInstant()),
                Timestamp.from(NOW.toInstant()),
                Timestamp.from(NOW.toInstant()));

        return transactionId;
    }

    private void insertCase(
            UUID paymentId,
            UUID transactionId,
            String status,
            String resolution,
            OffsetDateTime resolvedAt,
            String resolvedByType,
            String resolvedBy) {

        jdbcTemplate.update(
                """
                INSERT INTO reconciliation_cases (
                    id,
                    payment_id,
                    payment_transaction_id,
                    status,
                    reason,
                    resolution,
                    provider,
                    provider_reference,
                    opened_at,
                    resolved_at,
                    resolved_by_type,
                    resolved_by,
                    resolution_reason,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                bytes(UuidGenerator.next()),
                bytes(paymentId),
                bytes(transactionId),
                status,
                "REFUND_PROVIDER_UNKNOWN",
                resolution,
                "MOCK",
                null,
                Timestamp.from(NOW.toInstant()),
                resolvedAt == null ? null : Timestamp.from(resolvedAt.toInstant()),
                resolvedByType,
                resolvedBy,
                null,
                Timestamp.from(NOW.toInstant()),
                Timestamp.from(NOW.toInstant()));
    }

    private static byte[] bytes(UUID value) {

        byte[] bytes = new byte[16];

        long mostSignificantBits = value.getMostSignificantBits();
        long leastSignificantBits = value.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (mostSignificantBits >>> (8 * (7 - i)));

            bytes[8 + i] = (byte) (leastSignificantBits >>> (8 * (7 - i)));
        }

        return bytes;
    }
}
