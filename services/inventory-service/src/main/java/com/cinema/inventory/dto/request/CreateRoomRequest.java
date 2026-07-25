package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
                @NotBlank @Size(max = 100) String name,

                @NotNull RoomType roomType) {
}