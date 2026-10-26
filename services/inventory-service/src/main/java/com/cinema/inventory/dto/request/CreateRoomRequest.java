package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.RoomType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull RoomType roomType,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description = "Initial activity state. Omitted or null defaults to true.")
                Boolean active,
        @Size(max = 2)
                @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Supported content formats. Omitted or null defaults "
                                        + "to an empty list. Duplicate values are rejected.")
                List<@NotNull ContentFormat> contentFormats) {

    public CreateRoomRequest(String name, RoomType roomType) {
        this(name, roomType, null, null);
    }
}
