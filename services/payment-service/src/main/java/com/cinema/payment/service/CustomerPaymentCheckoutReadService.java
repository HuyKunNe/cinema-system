package com.cinema.payment.service;

import com.cinema.payment.controller.response.PaymentCheckoutResponse;

import java.util.UUID;

public interface CustomerPaymentCheckoutReadService {

    PaymentCheckoutResponse findByBookingId(UUID userId, UUID bookingId);
}
