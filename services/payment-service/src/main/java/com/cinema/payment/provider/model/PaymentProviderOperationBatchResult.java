package com.cinema.payment.provider.model;

public record PaymentProviderOperationBatchResult(
        int claimedCount, int appliedCount, int failedCount) {

    public static PaymentProviderOperationBatchResult empty() {

        return new PaymentProviderOperationBatchResult(0, 0, 0);
    }
}
