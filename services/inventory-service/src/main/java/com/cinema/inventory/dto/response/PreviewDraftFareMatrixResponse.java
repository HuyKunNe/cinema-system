package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixFareRuleResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PreviewDraftFareMatrixResponse(
        boolean simulation,
        boolean eligibilityVerified,
        boolean bookingQuote,
        boolean publicationValidated,
        CinemaPricingPolicyResponse policy,
        MatrixPreviewContext resolved,
        FareRuleSelectionStatus status,
        List<MatrixFareRuleResponse> winners) {

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
            List<String> simulatedEligibilityCodes,
            String requestedTicketCategoryCode,
            String categoryAdjustmentReason) {}
}
