package com.cinema.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Thông tin giá đã chốt từ snapshot của booking")
public record BookingPricingResponse(
        UUID pricingPolicyId,
        long pricingPolicyVersion,
        String timeZone,
        OffsetDateTime localStartsAt,
        String experienceType,
        String contentFormat,
        boolean advanceScreening,
        String currency,
        String totalAmountVnd,
        List<Line> items) {

    public BookingPricingResponse {
        items = List.copyOf(items);
    }

    public record Line(
            UUID showSeatId,
            UUID seatId,
            String seatNumber,
            String seatType,
            int capacity,
            String requestedTicketCategoryCode,
            String effectiveTicketCategoryCode,
            String effectiveTicketCategoryName,
            String categoryAdjustmentReason,
            UUID fareRuleId,
            String fareRuleCode,
            @Schema(description = "PER_TICKET: giá một vé; PER_PAIR: giá cả cặp") String fareUnit,
            @Schema(description = "Giá trọn gói, số nguyên VND dạng chuỗi") String amountVnd) {}
}
