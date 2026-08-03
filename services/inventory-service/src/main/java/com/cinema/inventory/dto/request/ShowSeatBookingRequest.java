package com.cinema.inventory.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ShowSeatBookingRequest(
        @NotNull UUID bookingId) {
}
