package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.PricingPolicyStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyResponse(
        UUID id,
        UUID cinemaId,
        @Schema(description = "Business policy version within this cinema.")
                long policyVersion,
        @Schema(description = "Optimistic-lock version, separate from policyVersion.")
                long version,
        PricingPolicyStatus status,
        String currency,
        String timeZone,
        LocalDate effectiveFrom,
        @Schema(nullable = true) LocalDate effectiveTo,
        @Schema(nullable = true) OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
