package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.enums.PricingDayGroup;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PricingFareQuoteResponse(
        UUID policyId,
        UUID policyVersionId,
        long revision,
        UUID cinemaId,
        String cinemaTimeZone,
                OffsetDateTime localStartsAt,
        PricingDayGroup dayGroup,
                int capacityPerUnit,
                CellResponse fare) {}
