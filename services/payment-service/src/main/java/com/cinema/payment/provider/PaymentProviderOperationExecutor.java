package com.cinema.payment.provider;

import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderRefundResult;

public interface PaymentProviderOperationExecutor {

    ProviderChargeResult execute(ClaimedProviderChargeOperation operation);

    ProviderRefundResult execute(ClaimedProviderRefundOperation operation);
}
