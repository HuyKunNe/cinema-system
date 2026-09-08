package com.cinema.payment.service;

import com.cinema.payment.provider.model.AppliedProviderChargeResult;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;

public interface PaymentProviderResultApplicationService {

    AppliedProviderChargeResult apply(
            ClaimedProviderChargeOperation operation, ProviderChargeResult result);
}
