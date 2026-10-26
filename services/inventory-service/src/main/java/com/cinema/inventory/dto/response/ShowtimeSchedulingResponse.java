package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;

public record ShowtimeSchedulingResponse(
        Long movieRevision,
        Integer durationMinutes,
        Integer breakMinutes,
        OffsetDateTime readyAt) {}
