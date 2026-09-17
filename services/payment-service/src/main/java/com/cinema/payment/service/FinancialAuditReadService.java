package com.cinema.payment.service;

import com.cinema.payment.controller.response.FinancialAuditRecordResponse;

import java.util.List;
import java.util.UUID;

public interface FinancialAuditReadService {

    List<FinancialAuditRecordResponse> findByPaymentId(UUID paymentId);
}
