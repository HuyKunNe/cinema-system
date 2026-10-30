package com.cinema.payment.controller;

import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.payment.controller.response.PaymentManagementResponse;
import com.cinema.payment.controller.response.PaymentTransactionManagementResponse;
import com.cinema.payment.service.PaymentManagementReadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments/management")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"payment:read"})
@Tag(name = "Payment management")
public class PaymentManagementController {

    private final PaymentManagementReadService paymentManagementReadService;

    public PaymentManagementController(PaymentManagementReadService paymentManagementReadService) {

        this.paymentManagementReadService = paymentManagementReadService;
    }

    @Operation(
            operationId = "listBookingPaymentsForAdmin",
            summary = "List payment attempts for a booking",
            description =
                    "Requires the ADMIN role and payment:read permission. "
                            + "Results are ordered by paymentAttempt descending, "
                            + "then id descending. "
                            + "An empty page means no matching payment record exists.")
    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<PageResponse<PaymentManagementResponse>>>
            listBookingPaymentsForAdmin(
                    @PathVariable("bookingId") @UuidV7 UUID bookingId,
                    @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                    @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        PageResponse<PaymentManagementResponse> response =
                paymentManagementReadService.findByBookingId(bookingId, page, size);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }

    @Operation(
            operationId = "getPaymentForAdmin",
            summary = "Get payment details for administrators",
            description = "Requires the ADMIN role and payment:read permission.")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentManagementResponse>> getPaymentForAdmin(
            @PathVariable("paymentId") @UuidV7 UUID paymentId) {

        PaymentManagementResponse response = paymentManagementReadService.findById(paymentId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }

    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionManagementResponse>>>
            listPaymentTransactionsForAdmin(
                    @PathVariable("paymentId") @UuidV7 UUID paymentId,
                    @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                    @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        PageResponse<PaymentTransactionManagementResponse> response =
                paymentManagementReadService.findTransactionsByPaymentId(paymentId, page, size);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }
}
