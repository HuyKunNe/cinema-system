package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.PricingDayGroup;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReplacePricingPolicyDraftRequest(
        @NotNull @Min(0) Long expectedVersion,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull PricingDayGroup dayGroup,
        @NotEmpty List<@NotNull UUID> cinemaIds) {}
