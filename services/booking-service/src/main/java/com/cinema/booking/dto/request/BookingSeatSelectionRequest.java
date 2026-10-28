package com.cinema.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(requiredProperties = {"seatNumber"})
public record BookingSeatSelectionRequest(
        @NotBlank @Size(max = 20) String seatNumber,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Requested ticket group from the assigned pricing "
                                        + "version. Null uses the default group.")
                UUID ticketGroupId) {}
