package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse.MatrixReviewIssue;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ValidateDraftFareMatrixResponse(
        CinemaPricingPolicyResponse policy,
        ValidationStatus status,
        boolean matrixValid,
        @Schema(description = "Always false. This result does not authorize publication.")
        boolean publicationValidated,
        long datesVisited,
        long cellsChecked,
        long ruleEvaluations,
        long findingCount,
        boolean findingsTruncated,
        boolean configurationReportTruncated,
        List<MatrixReviewIssue> configurationIssues,
        List<MatrixFinding> findings,
        List<String> uncheckedAreas) {

    public enum ValidationStatus {
        CONFIGURATION_ERRORS,
        COMPLETE,
        ANALYSIS_LIMIT_REACHED
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixFinding(
            String code,
            UUID scopeId,
            String scopeCode,
            LocalDate representativeDate,
            boolean holiday,
            int fromMinute,
            int toMinute,
            boolean advanceScreening,
            List<String> eligibilityCodes,
            List<UUID> winningFareRuleIds) {}
}
