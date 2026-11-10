package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull RoomType roomType,
                Boolean active,
        @Size(max = 2)
                List<@NotNull ContentFormat> contentFormats) {

    public CreateRoomRequest(String name, RoomType roomType) {
        this(name, roomType, null, null);
    }
}
