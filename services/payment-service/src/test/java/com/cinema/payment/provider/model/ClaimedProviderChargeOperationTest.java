package com.cinema.payment.provider.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class ClaimedProviderChargeOperationTest {

    private static final UUID TRANSACTION_ID = UuidGenerator.next();

    private static final String PROCESSING_OWNER = "payment-provider-operation:worker-1";

    private static final String IDEMPOTENCY_KEY = "charge:01991d2b-bd4a-7000-8000-000000000001";

    @Test
    void shouldCreateNormalizedImmutableOperation() {

        ProviderChargeCommand command = command();

        ClaimedProviderChargeOperation operation =
                new ClaimedProviderChargeOperation(
                        TRANSACTION_ID,
                        "  " + PROCESSING_OWNER + "  ",
                        " mock ",
                        "  " + IDEMPOTENCY_KEY + "  ",
                        command);

        assertThat(operation.transactionId()).isEqualTo(TRANSACTION_ID);

        assertThat(operation.processingOwner()).isEqualTo(PROCESSING_OWNER);

        assertThat(operation.provider()).isEqualTo("MOCK");

        assertThat(operation.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);

        assertThat(operation.command()).isSameAs(command);
    }

    @Test
    void missingTransactionIdShouldBeRejected() {

        assertValidation(
                () ->
                        new ClaimedProviderChargeOperation(
                                null, PROCESSING_OWNER, "MOCK", IDEMPOTENCY_KEY, command()),
                PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED);
    }

    @Test
    void missingProcessingOwnerShouldBeRejected() {

        assertValidation(
                () ->
                        new ClaimedProviderChargeOperation(
                                TRANSACTION_ID, " ", "MOCK", IDEMPOTENCY_KEY, command()),
                PaymentErrorCode.PROCESSING_OWNER_REQUIRED);
    }

    @Test
    void missingCommandShouldBeRejected() {

        assertValidation(
                () ->
                        new ClaimedProviderChargeOperation(
                                TRANSACTION_ID, PROCESSING_OWNER, "MOCK", IDEMPOTENCY_KEY, null),
                PaymentErrorCode.PROVIDER_CHARGE_COMMAND_REQUIRED);
    }

    private static ProviderChargeCommand command() {

        return new ProviderChargeCommand(
                UuidGenerator.next(),
                UuidGenerator.next(),
                new BigDecimal("125000.00"),
                "VND",
                OffsetDateTime.parse("2026-09-08T12:00:00Z"));
    }

    private static void assertValidation(
            ThrowingCallable callable, PaymentErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}
