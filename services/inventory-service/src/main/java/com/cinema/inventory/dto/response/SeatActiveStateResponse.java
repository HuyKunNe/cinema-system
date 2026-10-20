package com.cinema.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        description = "Current active state of a physical inventory seat.",
        requiredProperties = {"seatId", "roomId", "active"})
public record SeatActiveStateResponse(UUID seatId, UUID roomId, boolean active) {}
