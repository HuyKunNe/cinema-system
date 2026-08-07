package com.cinema.user.service;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public interface UserAccountLifecycleService {

    void verifyEmail(
            @NotNull UUID userId);

    void lock(
            @NotNull UUID userId);

    void unlock(
            @NotNull UUID userId);

    void disable(
            @NotNull UUID userId);

    void enable(
            @NotNull UUID userId);

    void recordSuccessfulLogin(
            @NotNull UUID userId);
}
