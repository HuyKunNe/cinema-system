package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceRoomContentFormatsRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotNull @Size(max = 2)
        List<@NotNull ContentFormat> formats) {}
