package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.enums.PricingDayGroup;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PricingFareQuoteResponse(
        UUID policyId,
        UUID policyVersionId,
        long revision,
        UUID cinemaId,
        String cinemaTimeZone,
        @Schema(description = "Screening start converted to the cinema time zone.")
                OffsetDateTime localStartsAt,
        PricingDayGroup dayGroup,
        @Schema(description = "Number of people accommodated by one sale unit.")
                int capacityPerUnit,
        @Schema(
                        description =
                                "Exact stored fare for one sale unit. "
                                        + "For PAIR, amount is the total price of the couple seat.")
                CellResponse fare) {}
