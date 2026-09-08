package com.cinema.payment.service;

import com.cinema.payment.entity.PaymentTransaction;

import java.util.List;

public interface PaymentTransactionClaimService {

    List<PaymentTransaction> claimNextBatch();
}
