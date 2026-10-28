package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        requiredProperties = {
            "showtimeId",
            "pricingPolicyVersionId",
            "timeBandId",
            "currency",
            "serverTime",
            "seatCount",
            "capacity",
            "totalAmount",
            "seats"
        })
public record ShowtimeGroupedSeatQuoteResponse(
        UUID showtimeId,
        UUID pricingPolicyVersionId,
        UUID timeBandId,
        String currency,
        OffsetDateTime serverTime,
        int seatCount,
        int capacity,
        BigDecimal totalAmount,
        List<SeatQuote> seats) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(
            requiredProperties = {
                "showSeatId",
                "seatNumber",
                "seatType",
                "capacity",
                "ticketGroupId",
                "ticketGroupName",
                "eligibilityDescription",
                "unit",
                "price"
            })
    public record SeatQuote(
            UUID showSeatId,
            String seatNumber,
            SeatType seatType,
            int capacity,
            UUID ticketGroupId,
            String ticketGroupName,
            @Schema(nullable = true) String eligibilityDescription,
            FareUnit unit,
            @Schema(
                            description =
                                    "Price for one inventory seat. "
                                            + "COUPLE uses the entire pair price.")
                    BigDecimal price) {}
}
