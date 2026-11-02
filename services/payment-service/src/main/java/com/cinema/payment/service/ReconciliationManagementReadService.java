package com.cinema.payment.service;

import com.cinema.common.response.model.PageResponse;
import com.cinema.payment.controller.response.PaymentReconciliationCaseResponse;
import com.cinema.payment.enums.ReconciliationStatus;

import java.util.UUID;

public interface ReconciliationManagementReadService {

    PageResponse<PaymentReconciliationCaseResponse> findAll(
            ReconciliationStatus status,
            UUID paymentId,
            int page,
            int size);

    PaymentReconciliationCaseResponse findById(UUID reconciliationCaseId);
}
