package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyVersionResponse(
        UUID id,
        UUID policyId,
        UUID cinemaId,
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
                String publishedBy) {

    public CinemaPricingPolicyVersionResponse(
            UUID id,
            UUID policyId,
            UUID cinemaId,
            long revision,
            String name,
            String description,
            PricingDayGroup dayGroup,
            PricingPolicyVersionStatus status,
            Long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        this(
                id,
                policyId,
                cinemaId,
                revision,
                name,
                description,
                dayGroup,
                status,
                version,
                createdAt,
                updatedAt,
                null,
                null);
    }
}
