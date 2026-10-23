package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;

import java.util.List;
import java.util.UUID;

public record RoomContentFormatsResponse(
        UUID roomId,
        Long version,
        List<ContentFormat> formats,
        List<ContentFormat> supportedFormatOptions) {}
