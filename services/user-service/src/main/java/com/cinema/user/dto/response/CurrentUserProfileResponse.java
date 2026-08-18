package com.cinema.user.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.user.enums.AccountStatus;

public record CurrentUserProfileResponse(
        UUID id,
        String email,
        String username,
        AccountStatus status,
        String firstName,
        String lastName,
        String phoneNumber,
        OffsetDateTime emailVerifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
