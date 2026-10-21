package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceCinemaPricingSaleScopesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest.StrictLongDeserializer
                                        .class)
                Long expectedVersion,
        @NotNull @Size(max = 2000) List<@NotNull @Valid SaleScopeInput> saleScopes) {

    public record SaleScopeInput(
            @NotBlank @Size(max = 64) String code,
            @NotNull ExperienceType experienceType,
            @NotNull ContentFormat contentFormat,
            @NotBlank @Size(max = 64) String dayGroupCode,
            @NotBlank @Size(max = 64) String timeBandCode,
            @NotBlank @Size(max = 64) String ticketCategoryCode,
            @NotBlank @Size(max = 64) String seatTypeCode,
            @NotNull AdvanceScreeningCondition advanceScreeningCondition) {}
}
