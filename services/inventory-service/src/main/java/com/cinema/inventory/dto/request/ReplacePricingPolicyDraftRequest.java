package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.PricingDayGroup;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReplacePricingPolicyDraftRequest(
        @NotNull @Min(0) Long expectedVersion,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull PricingDayGroup dayGroup) {}
