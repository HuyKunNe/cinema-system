package com.cinema.payment.provider;

import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderRefundCommand;
import com.cinema.payment.provider.model.ProviderRefundResult;

public interface PaymentProvider {

    String providerCode();

    ProviderChargeResult initiateCharge(ProviderChargeCommand command, String idempotencyKey);

    ProviderRefundResult initiateRefund(ProviderRefundCommand command, String idempotencyKey);
}
