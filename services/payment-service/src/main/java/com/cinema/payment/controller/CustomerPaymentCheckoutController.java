package com.cinema.payment.controller;

import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.payment.controller.response.PaymentCheckoutResponse;
import com.cinema.payment.service.CustomerPaymentCheckoutReadService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments/bookings")
public class CustomerPaymentCheckoutController {

    private final CustomerPaymentCheckoutReadService checkoutReadService;

    public CustomerPaymentCheckoutController(
            CustomerPaymentCheckoutReadService checkoutReadService) {
        this.checkoutReadService = checkoutReadService;
    }

    @GetMapping("/{bookingId}/checkout")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> getCheckoutForCurrentUser(
            @PathVariable("bookingId") @UuidV7 UUID bookingId) {

        PaymentCheckoutResponse response =
                checkoutReadService.findByBookingId(CurrentUser.id(), bookingId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }
}
