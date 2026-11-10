package com.cinema.inventory.dto.response;

import java.util.UUID;

public record SeatActiveStateResponse(UUID seatId, UUID roomId, boolean active) {}
