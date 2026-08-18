package com.cinema.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeCurrentUserPasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword) {

    @Override
    public String toString() {
        return "ChangeCurrentUserPasswordRequest[REDACTED]";
    }
}
