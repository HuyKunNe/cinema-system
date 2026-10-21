package com.cinema.inventory.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.inventory.enums.PricingPolicyModel;
import com.cinema.inventory.enums.PricingPolicyStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyResponse(
        UUID id,
        UUID cinemaId,
        @Schema(description = "Business policy version within this cinema.")
                long policyVersion,
        @Schema(description = "Optimistic-lock version.")
                long version,
        PricingPolicyStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "Immutable pricing model, separate from both version fields.")
                PricingPolicyModel pricingModel,
        String currency,
        String timeZone,
        LocalDate effectiveFrom,
        @Schema(nullable = true) LocalDate effectiveTo,
        @Schema(nullable = true) OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
