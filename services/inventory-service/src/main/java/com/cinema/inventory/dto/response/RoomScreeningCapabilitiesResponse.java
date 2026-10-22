package com.cinema.inventory.dto.response;

import com.cinema.common.api.screening.ScreeningFormat.ScreeningFormatOption;

import java.util.List;
import java.util.UUID;

public record RoomScreeningCapabilitiesResponse(
        UUID roomId,
        Long version,
        List<ScreeningFormatOption> configuredFormats,
        List<ScreeningFormatOption> supportedFormatOptions) {}
