package com.cinema.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmEmailVerificationRequest(
        @NotBlank @Size(min = 43, max = 43) String token,
        @NotBlank @Size(min = 12, max = 72) String password) {}
