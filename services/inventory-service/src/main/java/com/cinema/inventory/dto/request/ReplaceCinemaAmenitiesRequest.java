package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceCinemaAmenitiesRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotNull @Size(max = 30)
        List<@NotBlank @Size(max = 100) String> amenities) {}
