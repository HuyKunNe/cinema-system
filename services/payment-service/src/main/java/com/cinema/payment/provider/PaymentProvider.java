package com.cinema.payment.provider;

import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;

public interface PaymentProvider {

    String providerCode();

    ProviderChargeResult initiateCharge(ProviderChargeCommand command, String idempotencyKey);
}
