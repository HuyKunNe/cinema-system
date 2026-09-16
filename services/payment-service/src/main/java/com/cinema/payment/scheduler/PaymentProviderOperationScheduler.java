package com.cinema.payment.scheduler;

import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.service.PaymentProviderOperationWorker;
import com.cinema.payment.service.RefundPaymentProviderOperationWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cinema.payment.provider-operation",
        name = "scheduling-enabled",
        havingValue = "true")
public class PaymentProviderOperationScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaymentProviderOperationScheduler.class);

    private final PaymentProviderOperationWorker chargeWorker;

    private final RefundPaymentProviderOperationWorker refundWorker;

    public PaymentProviderOperationScheduler(
            PaymentProviderOperationWorker chargeWorker,
            RefundPaymentProviderOperationWorker refundWorker) {

        this.chargeWorker = chargeWorker;
        this.refundWorker = refundWorker;
    }

    @Scheduled(
            initialDelayString =
                    "${cinema.payment.provider-operation.scheduler-initial-delay:5s}",
            fixedDelayString =
                    "${cinema.payment.provider-operation.scheduler-delay:1s}")
    public void processNextBatch() {

        processChargeBatch();

        processRefundBatch();
    }

    private void processChargeBatch() {

        try {

            PaymentProviderOperationBatchResult result =
                    chargeWorker.processNextBatch();

            if (result.claimedCount() > 0) {

                LOGGER.info(
                        "Payment provider charge operation batch completed: "
                                + "claimedCount={}, appliedCount={}, failedCount={}",
                        result.claimedCount(),
                        result.appliedCount(),
                        result.failedCount());
            }

        } catch (RuntimeException exception) {

            /*
             * Do not allow a charge pipeline failure to prevent refund
             * processing during the same scheduler execution.
             *
             * Do not log unrestricted provider data or exception messages.
             */
            LOGGER.error(
                    "Payment provider charge operation batch failed: failureType={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void processRefundBatch() {

        try {

            PaymentProviderOperationBatchResult result =
                    refundWorker.processNextBatch();

            if (result.claimedCount() > 0) {

                LOGGER.info(
                        "Payment provider refund operation batch completed: "
                                + "claimedCount={}, appliedCount={}, failedCount={}",
                        result.claimedCount(),
                        result.appliedCount(),
                        result.failedCount());
            }

        } catch (RuntimeException exception) {

            /*
             * Do not allow a refund pipeline failure to terminate future
             * scheduler executions.
             *
             * Do not log unrestricted provider data or exception messages.
             */
            LOGGER.error(
                    "Payment provider refund operation batch failed: failureType={}",
                    exception.getClass().getSimpleName());
        }
    }
}
