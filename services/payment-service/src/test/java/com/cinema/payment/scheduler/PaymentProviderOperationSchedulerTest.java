package com.cinema.payment.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.service.PaymentProviderOperationWorker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProviderOperationSchedulerTest {

    @Mock private PaymentProviderOperationWorker operationWorker;

    private PaymentProviderOperationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentProviderOperationScheduler(operationWorker);
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
}
