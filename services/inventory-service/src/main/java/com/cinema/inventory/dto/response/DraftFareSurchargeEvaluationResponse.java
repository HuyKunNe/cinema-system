package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaFareSurchargesResponse.FareSurchargeResponse;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareUnit;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record DraftFareSurchargeEvaluationResponse(
        FareSurchargeMode mode,
        SurchargeEvaluationStatus status,
        @Schema(nullable = true) FareUnit fareUnit,
        @Schema(type = "string", nullable = true) String baseAmountVnd,
        @Schema(type = "string", nullable = true) String additionalAmountVnd,
        @Schema(type = "string", nullable = true) String combinedAmountVnd,
        List<FareSurchargeResponse> matchedRules,
        List<FareSurchargeResponse> selectedRules,
        @Schema(description = "Always false.")
        boolean movieAttributesVerified,
        List<String> simulatedMovieAttributeCodes) {

    public enum SurchargeEvaluationStatus {
        FARE_NOT_RESOLVED,
        NONE,
        MATCHED,
        AMBIGUOUS,
        UNIT_UNCONFIRMED,
        BOOKING_CONTEXT_REQUIRED
    }
}
