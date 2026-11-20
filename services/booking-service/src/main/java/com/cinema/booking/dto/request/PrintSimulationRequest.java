package com.cinema.booking.dto.request;

import com.cinema.booking.enums.PrintSimulationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PrintSimulationRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,100}") String deviceId,
        @NotNull PrintSimulationStatus outcome) {}
