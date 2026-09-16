package com.cinema.payment.service.impl;

import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.provider.PaymentProviderOperationExecutor;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;
import com.cinema.payment.service.RefundPaymentProviderOperationWorker;
import com.cinema.payment.service.RefundPaymentTransactionClaimService;
import com.cinema.payment.service.RefundProviderResultApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RefundPaymentProviderOperationWorkerImpl
        implements RefundPaymentProviderOperationWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RefundPaymentProviderOperationWorkerImpl.class);

    private final RefundPaymentTransactionClaimService claimService;

    private final PaymentProviderOperationPreparationService preparationService;

    private final PaymentProviderOperationExecutor operationExecutor;

    private final RefundProviderResultApplicationService resultApplicationService;

    public RefundPaymentProviderOperationWorkerImpl(
            RefundPaymentTransactionClaimService claimService,
            PaymentProviderOperationPreparationService preparationService,
            PaymentProviderOperationExecutor operationExecutor,
            RefundProviderResultApplicationService resultApplicationService) {

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

            ClaimedProviderRefundOperation operation =
                    preparationService.prepareRefund(
                            transaction.getId(), transaction.getProcessingOwner());

            ProviderRefundResult providerResult = operationExecutor.execute(operation);

            resultApplicationService.apply(operation, providerResult);

            return true;

        } catch (RuntimeException exception) {

            /*
             * Keep the lease on failure so the same refund transaction can
             * be reclaimed after expiration with the same idempotency key.
             *
             * Never log provider payloads, unrestricted failure messages,
             * credentials, or authorization data.
             */
            LOGGER.warn(
                    "Refund provider operation failed: " + "transactionId={}, failureType={}",
                    transaction.getId(),
                    exception.getClass().getSimpleName());

            return false;
        }
    }
}
