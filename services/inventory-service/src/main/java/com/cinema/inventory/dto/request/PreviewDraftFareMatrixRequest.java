package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Set;

public record PreviewDraftFareMatrixRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                @Schema(description = "Expected policy.version, not policyVersion.")
                Long expectedVersion,
        @NotNull
                @Schema(
                        description = "Hypothetical start time with ISO-8601 offset.",
                        example = "2026-11-03T18:30:00+07:00")
                OffsetDateTime startsAt,
        @NotNull ExperienceType experienceType,
        @NotNull ContentFormat contentFormat,
        @NotBlank @Size(max = 64) String ticketCategoryCode,
        @NotBlank @Size(max = 64) String seatTypeCode,
        @NotNull
                @Schema(
                        description =
                                "Hypothetical first-showing flag. "
                                        + "The server does not infer first showing.")
                Boolean simulatedFirstShowing,
        @Size(max = 20)
                @Schema(
                        description =
                                "Hypothetical satisfied eligibility codes. "
                                        + "Omission means none are satisfied.")
                Set<@NotBlank @Size(max = 64) String> simulatedEligibilityCodes) {}
