package com.cinema.inventory.dto.response;

import com.cinema.inventory.service.pricing.BasePriceEvaluationResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record PreviewDraftBasePriceResponse(
        @Schema(description = "Always true for this draft preview.")
                boolean simulation,
        @Schema(description = "Always false; surcharge calculation is not included.")
                boolean includesSurcharges,
        @Schema(description = "Always false; customer eligibility is not verified.")
                boolean eligibilityVerified,
        @Schema(description = "Always false; this result cannot be used as a booking quote.")
                boolean bookingQuote,
        BasePriceEvaluationResult result) {

    public static PreviewDraftBasePriceResponse basePriceSimulation(
            BasePriceEvaluationResult result) {

        return new PreviewDraftBasePriceResponse(
                true,
                false,
                false,
                false,
                result);
    }
}
