package com.cinema.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaFareMatrixReviewResponse(
        CinemaPricingPolicyResponse policy,
        boolean hasErrors,
        boolean reportTruncated,
        @Schema(
                        description =
                                "Always false. This report does not authorize publication.")
                boolean publicationValidated,
        List<MatrixReviewIssue> issues,
        List<String> uncheckedAreas) {

    public enum MatrixReviewSeverity {
        ERROR,
        WARNING
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixReviewIssue(
            MatrixReviewSeverity severity,
            String code,
            String message,
            List<UUID> relatedIds) {}
}
