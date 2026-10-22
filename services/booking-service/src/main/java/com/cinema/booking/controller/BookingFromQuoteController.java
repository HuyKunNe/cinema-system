package com.cinema.booking.controller;

import com.cinema.booking.dto.request.CreateBookingFromQuoteRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.service.BookingFromQuoteService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/bookings/from-quote")
@SecurityRequirement(name = "oauth2", scopes = {"booking:create"})
public class BookingFromQuoteController {

    private final BookingFromQuoteService service;

    public BookingFromQuoteController(BookingFromQuoteService service) {
        this.service = service;
    }

    @Operation(
            operationId = "createBookingFromQuote",
            summary = "Tạo đơn đặt vé từ báo giá",
            description =
                    """
                    Yêu cầu booking:create.
                    Chỉ gửi clientRequestId và quoteId.
                    Backend lấy suất chiếu và ghế từ báo giá của người hiện tại.

                    Trả 202 với trạng thái hiện tại của booking.
                    Booking mới bắt đầu ở PENDING, chưa có nghĩa đã giữ ghế.
                    Gửi lại cùng clientRequestId và quoteId trả booking cũ.
                    Giá chốt được ghi sau khi Inventory giữ ghế thành công.

                    Luồng này phụ thuộc cấu hình quote-booking.enabled.
                    """)
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody CreateBookingFromQuoteRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        BookingResponse response =
                service.create(
                        CurrentUser.id(),
                        request,
                        authorization);

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/bookings/" + response.id()))
                .body(ResponseFactory.success(response));
    }
}
