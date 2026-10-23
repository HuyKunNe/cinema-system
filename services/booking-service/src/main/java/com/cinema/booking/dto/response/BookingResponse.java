package com.cinema.booking.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.cinema.booking.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID showtimeId,
        String clientRequestId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt,
        String rejectionReason,
        List<BookingSeatResponse> seats,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        description =
                                "Quote gắn với booking. Null với booking legacy.")
                UUID pricingQuoteId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        description =
                                "Snapshot giá đã chốt sau khi giữ ghế thành công. "
                                        + "Null với booking legacy hoặc chưa có "
                                        + "kết quả reservation thành công.")
                BookingPricingResponse pricing) {

    public BookingResponse(
            UUID id,
            UUID userId,
            UUID showtimeId,
            String clientRequestId,
            BookingStatus status,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime expiresAt,
            OffsetDateTime confirmedAt,
            OffsetDateTime cancelledAt,
            String rejectionReason,
            List<BookingSeatResponse> seats,
            Long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        this(
                id,
                userId,
                showtimeId,
                clientRequestId,
                status,
                totalAmount,
                currency,
                expiresAt,
                confirmedAt,
                cancelledAt,
                rejectionReason,
                seats,
                version,
                createdAt,
                updatedAt,
                null,
                null);
    }
}
