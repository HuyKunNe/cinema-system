package com.cinema.user.dto.response;

import com.cinema.user.enums.AccountStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffAccountResponse(
        UUID userId,
        String username,
        String email,
        AccountStatus status,
        UUID cinemaId,
        UUID assignedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
