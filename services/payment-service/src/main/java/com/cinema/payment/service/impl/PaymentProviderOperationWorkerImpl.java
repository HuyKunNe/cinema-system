package com.cinema.payment.service.impl;

import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.provider.PaymentProviderOperationExecutor;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;
import com.cinema.payment.service.PaymentProviderOperationWorker;
import com.cinema.payment.service.PaymentProviderResultApplicationService;
import com.cinema.payment.service.PaymentTransactionClaimService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentProviderOperationWorkerImpl implements PaymentProviderOperationWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaymentProviderOperationWorkerImpl.class);

    private final PaymentTransactionClaimService claimService;

    private final PaymentProviderOperationPreparationService preparationService;

    private final PaymentProviderOperationExecutor operationExecutor;

    private final PaymentProviderResultApplicationService resultApplicationService;

    public PaymentProviderOperationWorkerImpl(
            PaymentTransactionClaimService claimService,
            PaymentProviderOperationPreparationService preparationService,
            PaymentProviderOperationExecutor operationExecutor,
            PaymentProviderResultApplicationService resultApplicationService) {

        this.claimService = claimService;
        this.preparationService = preparationService;
        this.operationExecutor = operationExecutor;
        this.resultApplicationService = resultApplicationService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public PaymentProviderOperationBatchResult processNextBatch() {

        List<PaymentTransaction> claimedTransactions = claimService.claimNextBatch();

        if (claimedTransactions.isEmpty()) {
            return PaymentProviderOperationBatchResult.empty();
        }

        int appliedCount = 0;
        int failedCount = 0;

        for (PaymentTransaction transaction : claimedTransactions) {
            if (processTransaction(transaction)) {
                appliedCount++;
            } else {
                failedCount++;
            }
        }

        return new PaymentProviderOperationBatchResult(
                claimedTransactions.size(), appliedCount, failedCount);
    }

    private boolean processTransaction(PaymentTransaction transaction) {

        try {
            ClaimedProviderChargeOperation operation =
                    preparationService.prepareCharge(
                            transaction.getId(), transaction.getProcessingOwner());

            ProviderChargeResult providerResult = operationExecutor.execute(operation);

            resultApplicationService.apply(operation, providerResult);

            return true;

        } catch (RuntimeException exception) {
            /*
             * Do not log exception messages or provider payloads here.
             * The processing lease remains on the transaction and can be
             * reclaimed after expiration with the same idempotency key.
             */
            LOGGER.warn(
                    "Payment provider operation failed: " + "transactionId={}, failureType={}",
                    transaction.getId(),
                    exception.getClass().getSimpleName());

            return false;
        }
    }
}
