package com.cinema.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class FinancialAuditRecordTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Test
    void shouldCreateCanonicalFinancialAuditRecord() {

        UUID paymentId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();

        FinancialAuditRecord record =
                new FinancialAuditRecord(
                        paymentId,
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditActorType.USER,
                        " user-123 ",
                        " customer requested refund ",
                        " source=admin-api ",
                        correlationId,
                        OCCURRED_AT);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getPaymentId()).isEqualTo(paymentId);
        assertThat(record.getAction()).isEqualTo(FinancialAuditAction.REFUND_REQUESTED);
        assertThat(record.getActorType()).isEqualTo(FinancialAuditActorType.USER);
        assertThat(record.getActorId()).isEqualTo("user-123");
        assertThat(record.getReason()).isEqualTo("customer requested refund");
        assertThat(record.getMetadata()).isEqualTo("source=admin-api");
        assertThat(record.getCorrelationId()).isEqualTo(correlationId);
        assertThat(record.getOccurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void blankOptionalValuesShouldNormalizeToNull() {

        FinancialAuditRecord record =
                new FinancialAuditRecord(
                        UuidGenerator.next(),
                        FinancialAuditAction.RECONCILIATION_OPENED,
                        FinancialAuditActorType.SYSTEM,
                        "payment-reconciliation-worker",
                        " ",
                        "",
                        UuidGenerator.next(),
                        OCCURRED_AT);

        assertThat(record.getReason()).isNull();
        assertThat(record.getMetadata()).isNull();
    }

    @Test
    void missingPaymentIdShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        null,
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingActionShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        null,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingActorTypeShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        null,
                                        "user-123",
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankActorIdShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        " ",
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void actorIdLongerThanMaximumShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "a".repeat(256),
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingCorrelationIdShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        null,
                                        null,
                                        null,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingOccurredAtShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        null,
                                        null,
                                        UuidGenerator.next(),
                                        null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reasonLongerThanMaximumShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        "a".repeat(501),
                                        null,
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void metadataLongerThanMaximumShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                new FinancialAuditRecord(
                                        UuidGenerator.next(),
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "user-123",
                                        null,
                                        "a".repeat(2001),
                                        UuidGenerator.next(),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }
}
