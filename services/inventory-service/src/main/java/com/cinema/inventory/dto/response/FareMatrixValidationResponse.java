package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.FareMatrixResponse.ScopeResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

public record FareMatrixValidationResponse(
        UUID policyId,
        UUID policyVersionId,
        long revision,
        Long version,
        PricingPolicyVersionStatus status,
        boolean valid,
        boolean canPublish,
        int requiredCellCount,
        int configuredCellCount,
        int missingCellCount,
        List<Issue> issues) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Issue(String code, String message, ScopeResponse scope, CellReference cell) {}

    public record CellReference(
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            UUID timeBandId,
            UUID ticketGroupId,
            SeatType seatType) {}
}
