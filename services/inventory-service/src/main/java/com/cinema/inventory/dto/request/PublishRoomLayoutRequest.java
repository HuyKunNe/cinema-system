package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PublishRoomLayoutRequest(@NotNull @PositiveOrZero Long expectedVersion) {}
