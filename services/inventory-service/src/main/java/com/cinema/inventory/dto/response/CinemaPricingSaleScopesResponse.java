package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingSaleScopesResponse(
        CinemaPricingPolicyResponse policy,
        List<SaleScopeResponse> saleScopes) {

    public record SaleScopeResponse(
            UUID id,
            String code,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            String dayGroupCode,
            String timeBandCode,
            String ticketCategoryCode,
            String seatTypeCode,
            AdvanceScreeningCondition advanceScreeningCondition) {}
}
