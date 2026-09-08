package com.cinema.payment.provider;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProviderOperationExecutorImpl implements PaymentProviderOperationExecutor {

    private final PaymentProviderRegistry providerRegistry;

    public PaymentProviderOperationExecutorImpl(PaymentProviderRegistry providerRegistry) {

        this.providerRegistry = providerRegistry;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ProviderChargeResult execute(ClaimedProviderChargeOperation operation) {

        if (operation == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_OPERATION_REQUIRED);
        }

        PaymentProvider provider = providerRegistry.getRequired(operation.provider());

        ProviderChargeResult result =
                provider.initiateCharge(operation.command(), operation.idempotencyKey());

        if (result == null) {
            throw new InternalServerException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        return result;
    }
}
