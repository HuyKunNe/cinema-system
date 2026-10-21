package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixFareRuleResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PreviewDraftFareMatrixResponse(
        @Schema(description = "Always true.") boolean simulation,
        @Schema(description = "Always false.") boolean eligibilityVerified,
        @Schema(
                        description =
                                "True only when fare and additional amounts "
                                        + "were calculated for one fare unit.")
                boolean additionalSurchargesEvaluated,
        @Schema(description = "Always false.") boolean bookingQuote,
        @Schema(description = "Always false.") boolean publicationValidated,
        CinemaPricingPolicyResponse policy,
        MatrixPreviewContext resolved,
        FareRuleSelectionStatus status,
        List<MatrixFareRuleResponse> winners,
        DraftFareSurchargeEvaluationResponse surchargeEvaluation) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixPreviewContext(
            OffsetDateTime localStartsAt,
            boolean holiday,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            String ticketCategoryCode,
            String seatTypeCode,
            LocalDate simulatedReleaseDate,
            boolean simulatedAdvanceScreening,
            List<String> simulatedEligibilityCodes) {}
}
