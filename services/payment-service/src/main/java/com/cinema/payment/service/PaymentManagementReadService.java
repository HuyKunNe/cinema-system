package com.cinema.payment.service;

import com.cinema.common.response.model.PageResponse;
import com.cinema.payment.controller.response.PaymentManagementResponse;
import com.cinema.payment.controller.response.PaymentTransactionManagementResponse;

import java.util.UUID;

public interface PaymentManagementReadService {

    PageResponse<PaymentManagementResponse> findByBookingId(UUID bookingId, int page, int size);

    PaymentManagementResponse findById(UUID paymentId);

    PageResponse<PaymentTransactionManagementResponse> findTransactionsByPaymentId(
            UUID paymentId, int page, int size);
}
