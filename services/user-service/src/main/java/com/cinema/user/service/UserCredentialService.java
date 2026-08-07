package com.cinema.user.service;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserCredentialService {

    void createCredential(
            @NotNull UUID userId,
            @NotBlank String rawPassword);

    void changePassword(
            @NotNull UUID userId,
            @NotBlank String currentRawPassword,
            @NotBlank String newRawPassword);

    boolean verifyPassword(
            @NotNull UUID userId,
            @NotBlank String rawPassword);
}
