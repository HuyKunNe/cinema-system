package com.cinema.payment.scheduler;

import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.service.PaymentProviderOperationWorker;

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

    public PaymentProviderOperationScheduler(PaymentProviderOperationWorker chargeWorker) {

        this.chargeWorker = chargeWorker;
    }

    @Scheduled(
            initialDelayString = "${cinema.payment.provider-operation.scheduler-initial-delay:5s}",
            fixedDelayString = "${cinema.payment.provider-operation.scheduler-delay:1s}")
    public void processNextBatch() {

        try {
            PaymentProviderOperationBatchResult result = chargeWorker.processNextBatch();

            if (result.claimedCount() > 0) {
                LOGGER.info(
                        "Payment provider charge operation batch completed: "
                                + "claimedCount={}, appliedCount={}, failedCount={}",
                        result.claimedCount(),
                        result.appliedCount(),
                        result.failedCount());
            }
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Payment provider charge operation batch failed: failureType={}",
                    exception.getClass().getSimpleName());
        }
    }
}
