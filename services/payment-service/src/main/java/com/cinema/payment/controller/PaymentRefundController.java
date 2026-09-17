package com.cinema.payment.controller;

import com.cinema.payment.controller.request.RefundPaymentRequest;
import com.cinema.payment.service.RefundRequestService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentRefundController {

    private final RefundRequestService refundRequestService;

    public PaymentRefundController(RefundRequestService refundRequestService) {

        this.refundRequestService = refundRequestService;
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<?> requestRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundPaymentRequest request,
            Authentication authentication) {

        /*
         * Do not accept actor identity from the client.
         *
         * actorId must come from the validated JWT principal:
         *
         * authentication.getName()
         *
         * Then map the HTTP request into the existing
         * RefundRequestService contract.
         */

        throw new UnsupportedOperationException(
                "Map to the existing RefundRequestService request contract");
    }
}
