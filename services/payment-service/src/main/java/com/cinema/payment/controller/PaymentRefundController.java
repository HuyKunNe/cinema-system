package com.cinema.payment.controller;

import com.cinema.payment.controller.request.RefundPaymentRequest;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;
import com.cinema.payment.service.RefundRequestService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentRefundController {

    private final RefundRequestService refundRequestService;

    public PaymentRefundController(
            RefundRequestService refundRequestService) {

        this.refundRequestService = refundRequestService;
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundRequestResult> requestRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundPaymentRequest request,
            Authentication authentication) {

        RefundRequest refundRequest =
                new RefundRequest(
                        paymentId,
                        FinancialAuditActorType.USER,
                        authentication.getName(),
                        request.reason(),
                        request.correlationId(),
                        OffsetDateTime.now(ZoneOffset.UTC));

        RefundRequestResult result =
                refundRequestService.requestRefund(refundRequest);

        return ResponseEntity.ok(result);
    }
}
