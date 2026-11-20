package com.cinema.user.dto.request;

import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignStaffCinemaRequest(@NotNull @UuidV7 UUID cinemaId) {}
