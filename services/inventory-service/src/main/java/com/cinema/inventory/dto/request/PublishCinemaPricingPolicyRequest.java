package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PublishCinemaPricingPolicyRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest.StrictLongDeserializer
                                        .class)
                @Schema(description = "Expected policy.version, not policyVersion.")
                Long expectedVersion,
        @NotNull
                @AssertTrue
                @Schema(
                        description =
                                "Explicit confirmation that tariff amounts, promotion "
                                        + "conditions, sale scopes, seat mappings, holiday "
                                        + "calendar and eligibility-code compatibility "
                                        + "have been reviewed for this policy version.")
                Boolean businessConfirmed,
        @NotBlank
                @Size(max = 500)
                @Schema(
                        description =
                                "Reference to the approved tariff/business configuration. "
                                        + "This is recorded with publisher identity and time.")
                String businessConfirmationReference) {}
