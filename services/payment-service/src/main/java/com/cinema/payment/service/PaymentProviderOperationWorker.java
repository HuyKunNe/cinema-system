package com.cinema.payment.service;

import com.cinema.payment.provider.model.PaymentProviderOperationBatchResult;

public interface PaymentProviderOperationWorker {

    PaymentProviderOperationBatchResult processNextBatch();
}
