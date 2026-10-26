package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublishPricingPolicyRequest(@NotNull @Min(0) Long expectedVersion) {}
