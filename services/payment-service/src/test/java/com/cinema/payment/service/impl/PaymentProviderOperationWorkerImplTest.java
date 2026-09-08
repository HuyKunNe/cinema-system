package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.PaymentProviderOperationExecutor;
import com.cinema.payment.provider.model.AppliedProviderChargeResult;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;
import com.cinema.payment.service.PaymentProviderResultApplicationService;
import com.cinema.payment.service.PaymentTransactionClaimService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentProviderOperationWorkerImplTest {

    private static final String PROCESSING_OWNER = "payment-provider-operation:worker-1";

    @Mock private PaymentTransactionClaimService claimService;

    @Mock private PaymentProviderOperationPreparationService preparationService;

    @Mock private PaymentProviderOperationExecutor operationExecutor;

    @Mock private PaymentProviderResultApplicationService resultApplicationService;

    @Mock private PaymentTransaction firstTransaction;

    @Mock private PaymentTransaction secondTransaction;

    private PaymentProviderOperationWorkerImpl worker;

    @BeforeEach
    void setUp() {

        worker =
                new PaymentProviderOperationWorkerImpl(
                        claimService,
                        preparationService,
                        operationExecutor,
                        resultApplicationService);
    }

    @Test
    void shouldProcessOperationUsingRequiredBoundaryOrder() {

        UUID transactionId = UuidGenerator.next();

        ClaimedProviderChargeOperation operation = operation(transactionId);

        ProviderChargeResult providerResult =
                ProviderChargeResult.succeeded("mock-provider-reference");

        AppliedProviderChargeResult appliedResult =
                new AppliedProviderChargeResult(
                        operation.command().paymentId(),
                        transactionId,
                        PaymentStatus.SUCCEEDED,
                        PaymentTransactionStatus.SUCCEEDED);

        when(claimService.claimNextBatch()).thenReturn(List.of(firstTransaction));

        when(firstTransaction.getId()).thenReturn(transactionId);

        when(firstTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(preparationService.prepareCharge(transactionId, PROCESSING_OWNER))
                .thenReturn(operation);

        when(operationExecutor.execute(operation)).thenReturn(providerResult);

        when(resultApplicationService.apply(operation, providerResult)).thenReturn(appliedResult);

        PaymentProviderOperationBatchResult result = worker.processNextBatch();

        assertThat(result.claimedCount()).isEqualTo(1);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        InOrder order =
                inOrder(
                        claimService,
                        preparationService,
                        operationExecutor,
                        resultApplicationService);

        order.verify(claimService).claimNextBatch();

        order.verify(preparationService).prepareCharge(transactionId, PROCESSING_OWNER);

        order.verify(operationExecutor).execute(operation);

        order.verify(resultApplicationService).apply(operation, providerResult);
    }

    @Test
    void oneFailedOperationShouldNotStopRemainingBatch() {

        UUID firstTransactionId = UuidGenerator.next();
        UUID secondTransactionId = UuidGenerator.next();

        ClaimedProviderChargeOperation firstOperation = operation(firstTransactionId);

        ClaimedProviderChargeOperation secondOperation = operation(secondTransactionId);

        ProviderChargeResult secondProviderResult =
                ProviderChargeResult.succeeded("mock-second-reference");

        when(claimService.claimNextBatch())
                .thenReturn(List.of(firstTransaction, secondTransaction));

        when(firstTransaction.getId()).thenReturn(firstTransactionId);

        when(firstTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(secondTransaction.getId()).thenReturn(secondTransactionId);

        when(secondTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(preparationService.prepareCharge(firstTransactionId, PROCESSING_OWNER))
                .thenReturn(firstOperation);

        when(preparationService.prepareCharge(secondTransactionId, PROCESSING_OWNER))
                .thenReturn(secondOperation);

        when(operationExecutor.execute(firstOperation))
                .thenThrow(new InternalServerException(PaymentErrorCode.PROVIDER_RESULT_INVALID));

        when(operationExecutor.execute(secondOperation)).thenReturn(secondProviderResult);

        when(resultApplicationService.apply(secondOperation, secondProviderResult))
                .thenReturn(
                        new AppliedProviderChargeResult(
                                secondOperation.command().paymentId(),
                                secondTransactionId,
                                PaymentStatus.SUCCEEDED,
                                PaymentTransactionStatus.SUCCEEDED));

        PaymentProviderOperationBatchResult result = worker.processNextBatch();

        assertThat(result.claimedCount()).isEqualTo(2);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);

        verify(resultApplicationService, never()).apply(eq(firstOperation), any());

        verify(resultApplicationService).apply(secondOperation, secondProviderResult);
    }

    @Test
    void emptyClaimBatchShouldReturnEmptyResult() {

        when(claimService.claimNextBatch()).thenReturn(List.of());

        PaymentProviderOperationBatchResult result = worker.processNextBatch();

        assertThat(result).isEqualTo(PaymentProviderOperationBatchResult.empty());

        verify(preparationService, never()).prepareCharge(any(), any());

        verify(operationExecutor, never()).execute(any());

        verify(resultApplicationService, never()).apply(any(), any());
    }

    private static ClaimedProviderChargeOperation operation(UUID transactionId) {

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        new BigDecimal("125000.00"),
                        "VND",
                        OffsetDateTime.parse("2026-09-08T12:00:00Z"));

        return new ClaimedProviderChargeOperation(
                transactionId, PROCESSING_OWNER, "MOCK", "charge:" + transactionId, command);
    }
}
