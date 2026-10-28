package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        description =
                "Published ticket prices for the showtime's assigned pricing "
                        + "version and cinema-local screening time. "
                        + "Price applicability does not indicate seat availability.",
        requiredProperties = {
            "showtimeId",
            "cinemaId",
            "pricingPolicyVersionId",
            "pricingPolicyRevision",
            "dayGroup",
            "experienceType",
            "contentFormat",
            "currency",
            "timeZone",
            "localStartsAt",
            "timeBandId",
            "timeBandName",
            "defaultTicketGroupId",
            "ticketGroups"
        })
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
    @Schema(
            requiredProperties = {
                "ticketGroupId",
                "name",
                "eligibilityDescription",
                "defaultGroup",
                "fares"
            })
    public record TicketGroupPrices(
            UUID ticketGroupId,
            String name,
            @Schema(nullable = true) String eligibilityDescription,
            boolean defaultGroup,
            List<SeatFare> fares) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(requiredProperties = {"seatType", "capacity", "unit", "applicable", "price"})
    public record SeatFare(
            SeatType seatType,
            int capacity,
            FareUnit unit,
            boolean applicable,
            @Schema(
                            nullable = true,
                            description =
                                    "Price per inventory seat; COUPLE uses the "
                                            + "entire pair price. Null when not applicable.")
                    BigDecimal price) {}
}
