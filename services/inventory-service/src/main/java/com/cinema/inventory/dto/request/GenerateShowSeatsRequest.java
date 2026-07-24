package com.cinema.inventory.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record GenerateShowSeatsRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal defaultPrice) {
}