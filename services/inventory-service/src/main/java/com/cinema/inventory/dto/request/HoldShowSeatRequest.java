package com.cinema.inventory.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record HoldShowSeatRequest(
        @NotNull UUID bookingId,
        @NotNull @Future OffsetDateTime expiresAt) {
}
