package com.cinema.payment.service;

import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;

import java.util.UUID;

public interface PaymentProviderOperationPreparationService {

    ClaimedProviderChargeOperation prepareCharge(UUID transactionId, String processingOwner);
}
