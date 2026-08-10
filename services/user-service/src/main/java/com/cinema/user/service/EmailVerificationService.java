package com.cinema.user.service;

import java.util.UUID;

import com.cinema.user.service.model.IssuedEmailVerificationToken;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface EmailVerificationService {

    IssuedEmailVerificationToken issue(@NotNull UUID userId);

    void confirm(@NotBlank String rawToken);
}
