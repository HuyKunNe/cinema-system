package com.cinema.payment.service;

import com.cinema.payment.entity.Payment;

public interface PaymentResultOutboxService {

    void persistIfTerminal(Payment payment);
}
