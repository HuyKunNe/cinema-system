package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        OffsetDateTime publishedAt,
                String publishedBy) {}
