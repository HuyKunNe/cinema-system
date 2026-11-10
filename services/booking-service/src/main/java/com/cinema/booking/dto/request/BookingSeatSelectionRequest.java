package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BookingSeatSelectionRequest(
        @NotBlank @Size(max = 20) String seatNumber,
                UUID ticketGroupId) {}
