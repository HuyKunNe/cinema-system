package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CopyPublishedRoomLayoutRequest(
        @NotNull
                @PositiveOrZero
                Long expectedVersion) {}
