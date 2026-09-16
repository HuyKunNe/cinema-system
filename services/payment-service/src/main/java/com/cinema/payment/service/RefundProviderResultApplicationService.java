package com.cinema.payment.service;

import com.cinema.payment.provider.model.AppliedProviderRefundResult;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderRefundResult;

public interface RefundProviderResultApplicationService {

    AppliedProviderRefundResult apply(
            ClaimedProviderRefundOperation operation, ProviderRefundResult result);
}
