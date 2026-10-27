package com.cinema.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReplaceShowtimePricingRequest(
        @NotNull @Min(0) Long expectedVersion,
        @Schema(
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        description =
                                "Exact published pricing version. "
                                        + "Null or omitted clears the selection "
                                        + "while the showtime is SCHEDULED.")
                UUID pricingPolicyVersionId) {}
