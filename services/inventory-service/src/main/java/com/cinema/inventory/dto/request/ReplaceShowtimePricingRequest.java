package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReplaceShowtimePricingRequest(
        @NotNull @Min(0) Long expectedVersion,
                UUID pricingPolicyVersionId) {}
