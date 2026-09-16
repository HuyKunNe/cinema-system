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
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.PaymentProviderOperationExecutor;
import com.cinema.payment.provider.model.AppliedProviderRefundResult;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.provider.model.ProviderRefundCommand;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;
import com.cinema.payment.service.RefundPaymentTransactionClaimService;
import com.cinema.payment.service.RefundProviderResultApplicationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RefundPaymentProviderOperationWorkerImplTest {

    private static final String PROCESSING_OWNER = "payment-refund-provider-operation:worker-1";

    @Mock private RefundPaymentTransactionClaimService claimService;

    @Mock private PaymentProviderOperationPreparationService preparationService;

    @Mock private PaymentProviderOperationExecutor operationExecutor;

    @Mock private RefundProviderResultApplicationService resultApplicationService;

    @Mock private PaymentTransaction firstTransaction;

    @Mock private PaymentTransaction secondTransaction;

    private RefundPaymentProviderOperationWorkerImpl worker;

    @BeforeEach
    void setUp() {

        worker =
                new RefundPaymentProviderOperationWorkerImpl(
                        claimService,
                        preparationService,
                        operationExecutor,
                        resultApplicationService);
    }

    @Test
    void shouldProcessRefundUsingRequiredBoundaryOrder() {

        UUID transactionId = UuidGenerator.next();

        ClaimedProviderRefundOperation operation = operation(transactionId);

        ProviderRefundResult providerResult =
                ProviderRefundResult.succeeded("mock-refund-reference");

        AppliedProviderRefundResult appliedResult =
                new AppliedProviderRefundResult(
                        operation.command().paymentId(),
                        transactionId,
                        RefundStatus.SUCCEEDED,
                        PaymentTransactionStatus.SUCCEEDED);

        when(claimService.claimNextBatch()).thenReturn(List.of(firstTransaction));

        when(firstTransaction.getId()).thenReturn(transactionId);

        when(firstTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(preparationService.prepareRefund(transactionId, PROCESSING_OWNER))
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

        order.verify(preparationService).prepareRefund(transactionId, PROCESSING_OWNER);

        order.verify(operationExecutor).execute(operation);

        order.verify(resultApplicationService).apply(operation, providerResult);
    }

    @Test
    void oneFailedRefundShouldNotStopRemainingBatch() {

        UUID firstTransactionId = UuidGenerator.next();
        UUID secondTransactionId = UuidGenerator.next();

        ClaimedProviderRefundOperation firstOperation = operation(firstTransactionId);

        ClaimedProviderRefundOperation secondOperation = operation(secondTransactionId);

        ProviderRefundResult secondProviderResult =
                ProviderRefundResult.succeeded("mock-second-refund-reference");

        when(claimService.claimNextBatch())
                .thenReturn(List.of(firstTransaction, secondTransaction));

        when(firstTransaction.getId()).thenReturn(firstTransactionId);

        when(firstTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(secondTransaction.getId()).thenReturn(secondTransactionId);

        when(secondTransaction.getProcessingOwner()).thenReturn(PROCESSING_OWNER);

        when(preparationService.prepareRefund(firstTransactionId, PROCESSING_OWNER))
                .thenReturn(firstOperation);

        when(preparationService.prepareRefund(secondTransactionId, PROCESSING_OWNER))
                .thenReturn(secondOperation);

        when(operationExecutor.execute(firstOperation))
                .thenThrow(new InternalServerException(PaymentErrorCode.PROVIDER_RESULT_INVALID));

        when(operationExecutor.execute(secondOperation)).thenReturn(secondProviderResult);

        when(resultApplicationService.apply(secondOperation, secondProviderResult))
                .thenReturn(
                        new AppliedProviderRefundResult(
                                secondOperation.command().paymentId(),
                                secondTransactionId,
                                RefundStatus.SUCCEEDED,
                                PaymentTransactionStatus.SUCCEEDED));

        PaymentProviderOperationBatchResult result = worker.processNextBatch();

        assertThat(result.claimedCount()).isEqualTo(2);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);

        verify(resultApplicationService, never()).apply(eq(firstOperation), any());

        verify(resultApplicationService).apply(secondOperation, secondProviderResult);
    }

    @Test
    void emptyRefundClaimBatchShouldReturnEmptyResult() {

        when(claimService.claimNextBatch()).thenReturn(List.of());

        PaymentProviderOperationBatchResult result = worker.processNextBatch();

        assertThat(result).isEqualTo(PaymentProviderOperationBatchResult.empty());

        verify(preparationService, never()).prepareRefund(any(), any());

        verify(operationExecutor, never()).execute(any(ClaimedProviderRefundOperation.class));

        verify(resultApplicationService, never()).apply(any(), any());
    }

    private static ClaimedProviderRefundOperation operation(UUID transactionId) {

        UUID paymentId = UuidGenerator.next();

        ProviderRefundCommand command =
                new ProviderRefundCommand(
                        paymentId, "mock-charge-reference", new BigDecimal("125000.00"), "VND");

        return new ClaimedProviderRefundOperation(
                transactionId, PROCESSING_OWNER, "MOCK", "refund:" + paymentId, command);
    }
}
