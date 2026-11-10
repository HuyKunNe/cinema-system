package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShowtimeTicketPricesResponse(
        UUID showtimeId,
        UUID cinemaId,
        UUID pricingPolicyVersionId,
        long pricingPolicyRevision,
        PricingDayGroup dayGroup,
        PricingExperienceType experienceType,
        ContentFormat contentFormat,
        String currency,
        String timeZone,
        OffsetDateTime localStartsAt,
        UUID timeBandId,
        String timeBandName,
        UUID defaultTicketGroupId,
        List<TicketGroupPrices> ticketGroups) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TicketGroupPrices(
            UUID ticketGroupId,
            String name,
            String eligibilityDescription,
            boolean defaultGroup,
            List<SeatFare> fares) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SeatFare(
            SeatType seatType,
            int capacity,
            FareUnit unit,
            boolean applicable,
                    BigDecimal price) {}
}
