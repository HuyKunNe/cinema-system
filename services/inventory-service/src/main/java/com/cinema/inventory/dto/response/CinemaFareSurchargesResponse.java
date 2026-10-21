package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareSurchargeUnit;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaFareSurchargesResponse(
        CinemaPricingPolicyResponse policy,
        FareSurchargeMode mode,
        List<FareSurchargeResponse> surcharges) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record FareSurchargeResponse(
            UUID id,
            String code,
            String fareRuleCode,
            @Schema(nullable = true) String movieAttributeCode,
            @Schema(nullable = true) String requiredEligibilityCode,
            @Schema(type = "string", pattern = "^(0|[1-9][0-9]*)$")
                    String amountVnd,
            FareSurchargeUnit unit,
            int priority,
            LocalDate effectiveFrom,
            @Schema(nullable = true) LocalDate effectiveTo) {}
}
