package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyVersionResponse(
        UUID id,
        UUID policyId,
        List<UUID> cinemaIds,
        long revision,
        String name,
        String description,
        PricingDayGroup dayGroup,
        PricingPolicyVersionStatus status,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @Schema(nullable = true) OffsetDateTime publishedAt,
        @Schema(
                        nullable = true,
                        description = "Validated JWT subject that first published this version.")
                String publishedBy) {}
