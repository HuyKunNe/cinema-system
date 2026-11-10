package com.cinema.payment.controller;

import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.payment.controller.response.PaymentManagementResponse;
import com.cinema.payment.controller.response.PaymentTransactionManagementResponse;
import com.cinema.payment.service.PaymentManagementReadService;

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
public class PaymentManagementController {

    private final PaymentManagementReadService paymentManagementReadService;

    public PaymentManagementController(PaymentManagementReadService paymentManagementReadService) {

        this.paymentManagementReadService = paymentManagementReadService;
    }

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
