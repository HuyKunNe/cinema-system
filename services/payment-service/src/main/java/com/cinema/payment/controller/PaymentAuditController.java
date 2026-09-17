package com.cinema.payment.controller;

import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.service.FinancialAuditReadService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentAuditController {

    private final FinancialAuditReadService financialAuditReadService;

    public PaymentAuditController(FinancialAuditReadService financialAuditReadService) {

        this.financialAuditReadService = financialAuditReadService;
    }

    @GetMapping("/{paymentId}/audit")
    public ResponseEntity<List<FinancialAuditRecordResponse>> findAuditByPaymentId(
            @PathVariable UUID paymentId) {

        return ResponseEntity.ok(financialAuditReadService.findByPaymentId(paymentId));
    }
}
