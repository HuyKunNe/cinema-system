package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FareMatrixResponse(
        UUID policyId,
        UUID policyVersionId,
        long revision,
        Long version,
        PricingDayGroup dayGroup,
        PricingPolicyVersionStatus status,
        List<ScopeResponse> scopes,
        List<TimeBandResponse> timeBands,
        List<TicketGroupResponse> ticketGroups,
        UUID defaultTicketGroupId,
        List<SeatType> seatTypes,
        int requiredCellCount,
        int configuredCellCount,
        int missingCellCount,
        boolean coverageComplete,
        List<CellResponse> cells,
        Options options) {

    public record ScopeResponse(
            PricingExperienceType experienceType, ContentFormat contentFormat) {}

    public record TimeBandResponse(
            UUID id, String name, int startMinute, int endMinute, int sortOrder) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TicketGroupResponse(
            UUID id,
            String name,
            String eligibilityDescription,
            boolean defaultGroup,
            int sortOrder) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CellResponse(
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            UUID timeBandId,
            UUID ticketGroupId,
            SeatType seatType,
            FareCellState state,
            BigDecimal amount,
            FareUnit unit) {}

    public record SeatTypeOption(SeatType seatType, FareUnit unit, int capacity) {}

    public record Options(
            List<PricingExperienceType> experienceTypes,
            List<ContentFormat> contentFormats,
            List<SeatTypeOption> seatTypes,
            List<FareCellState> cellStates) {}
}
