package com.cinema.payment.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.service.PaymentProviderOperationWorker;
import com.cinema.payment.service.RefundPaymentProviderOperationWorker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProviderOperationSchedulerTest {

    @Mock private PaymentProviderOperationWorker operationWorker;

    private PaymentProviderOperationScheduler scheduler;

    @Mock private RefundPaymentProviderOperationWorker refundWorker;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentProviderOperationScheduler(operationWorker, refundWorker);
    }

    @Test
    void shouldProcessOneBoundedBatch() {
        PaymentProviderOperationBatchResult batchResult =
                new PaymentProviderOperationBatchResult(3, 2, 1);

        when(operationWorker.processNextBatch()).thenReturn(batchResult);

        scheduler.processNextBatch();

        verify(operationWorker).processNextBatch();
    }

    @Test
    void emptyBatchShouldCompleteNormally() {
        when(operationWorker.processNextBatch())
                .thenReturn(PaymentProviderOperationBatchResult.empty());

        assertThatCode(scheduler::processNextBatch).doesNotThrowAnyException();

        verify(operationWorker).processNextBatch();
    }

    @Test
    void topLevelWorkerFailureShouldNotEscapeSchedulerBoundary() {
        when(operationWorker.processNextBatch())
                .thenThrow(new IllegalStateException("simulated claim failure"));

        assertThatCode(scheduler::processNextBatch).doesNotThrowAnyException();

        verify(operationWorker).processNextBatch();
    }

    @Test
    void shouldProcessChargeAndRefundBatches() {

        when(operationWorker.processNextBatch())
                .thenReturn(new PaymentProviderOperationBatchResult(1, 1, 0));

        when(refundWorker.processNextBatch())
                .thenReturn(new PaymentProviderOperationBatchResult(1, 1, 0));

        scheduler.processNextBatch();

        verify(operationWorker).processNextBatch();

        verify(refundWorker).processNextBatch();
    }

    @Test
    void chargeFailureShouldNotPreventRefundProcessing() {

        when(operationWorker.processNextBatch())
                .thenThrow(new RuntimeException("forced charge failure"));

        when(refundWorker.processNextBatch())
                .thenReturn(PaymentProviderOperationBatchResult.empty());

        scheduler.processNextBatch();

        verify(operationWorker).processNextBatch();

        verify(refundWorker).processNextBatch();
    }

    @Test
    void refundFailureShouldNotEscapeSchedulerExecution() {

        when(operationWorker.processNextBatch())
                .thenReturn(PaymentProviderOperationBatchResult.empty());

        when(refundWorker.processNextBatch())
                .thenThrow(new RuntimeException("forced refund failure"));

        scheduler.processNextBatch();

        verify(operationWorker).processNextBatch();

        verify(refundWorker).processNextBatch();
    }
}
