package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixFareRuleResponse;
import com.cinema.inventory.dto.response.PreviewDraftFareMatrixResponse.MatrixPreviewContext;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record DraftFareMatrixCoverageResponse(
        CinemaPricingPolicyResponse policy,
        boolean simulation,
        boolean eligibilityVerified,
        boolean additionalSurchargesEvaluated,
        boolean bookingQuote,
        boolean publicationValidated,
        @Schema(
                        description =
                                "Always true. Segments describe local clock minutes, "
                                        + "not validated scheduling instants.")
                boolean localWallClockOnly,
        MatrixPreviewContext referenceContext,
        int fromMinute,
        int toMinute,
        boolean hasGaps,
        boolean hasAmbiguities,
        boolean hasUnconfirmedUnits,
        boolean segmentsTruncated,
        List<MatrixCoverageSegment> segments) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixCoverageSegment(
            int fromMinute,
            int toMinute,
            FareRuleSelectionStatus status,
            List<MatrixFareRuleResponse> winningRules) {}
}
