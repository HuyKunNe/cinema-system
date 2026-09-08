package com.cinema.payment.provider;

import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;

public interface PaymentProviderOperationExecutor {

    ProviderChargeResult execute(ClaimedProviderChargeOperation operation);
}
