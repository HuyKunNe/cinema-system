package com.cinema.booking.dto.request;

import com.cinema.common.validation.annotation.UuidV5OrV7;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotBlank(message = "Client request ID is required")
                @Size(max = 100, message = "Client request ID must not exceed 100 characters")
                String clientRequestId,
        @NotNull(message = "Showtime ID is required")
                @UuidV5OrV7
                @Schema(
                        description =
                                "Showtime resource ID: UUID v5 for imported data "
                                        + "or UUID v7 for generated data",
                        format = "uuid")
                UUID showtimeId,
        @NotEmpty(message = "At least one seat number is required") @Valid
                List<
                                @NotBlank(message = "Seat number is required")
                                @Size(
                                        max = 20,
                                        message = "Seat number must not exceed 20 characters")
                                String>
                        seatNumbers) {}
