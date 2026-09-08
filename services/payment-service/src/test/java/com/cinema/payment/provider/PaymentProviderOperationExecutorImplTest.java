package com.cinema.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class PaymentProviderOperationExecutorImplTest {

    @Mock private PaymentProviderRegistry providerRegistry;

    @Mock private PaymentProvider paymentProvider;

    private PaymentProviderOperationExecutorImpl executor;

    @BeforeEach
    void setUp() {

        executor = new PaymentProviderOperationExecutorImpl(providerRegistry);
    }

    @Test
    void shouldExecuteChargeUsingResolvedProviderAndStableKey() {

        ClaimedProviderChargeOperation operation = operation();

        ProviderChargeResult expectedResult =
                ProviderChargeResult.succeeded("mock-provider-reference");

        when(providerRegistry.getRequired("MOCK")).thenReturn(paymentProvider);

        when(paymentProvider.initiateCharge(operation.command(), operation.idempotencyKey()))
                .thenReturn(expectedResult);

        ProviderChargeResult actualResult = executor.execute(operation);

        assertThat(actualResult).isSameAs(expectedResult);

        verify(providerRegistry).getRequired("MOCK");

        verify(paymentProvider).initiateCharge(operation.command(), operation.idempotencyKey());
    }

    @Test
    void missingOperationShouldBeRejected() {

        assertThatThrownBy(() -> executor.execute(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_OPERATION_REQUIRED));
    }

    @Test
    void nullProviderResultShouldBeRejectedAsSystemFailure() {

        ClaimedProviderChargeOperation operation = operation();

        when(providerRegistry.getRequired("MOCK")).thenReturn(paymentProvider);

        when(paymentProvider.initiateCharge(operation.command(), operation.idempotencyKey()))
                .thenReturn(null);

        assertThatThrownBy(() -> executor.execute(operation))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(((InternalServerException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PROVIDER_RESULT_INVALID));
    }

    private static ClaimedProviderChargeOperation operation() {

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        new BigDecimal("125000.00"),
                        "VND",
                        OffsetDateTime.parse("2026-09-08T12:00:00Z"));

        return new ClaimedProviderChargeOperation(
                UuidGenerator.next(),
                "payment-provider-operation:worker-1",
                "MOCK",
                "charge:01991d2b-bd4a-7000-8000-000000000001",
                command);
    }
}
