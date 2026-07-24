package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.SeatType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeatRequest(
        @NotBlank
        @Size(max = 20)
        String seatNumber,

        @NotBlank
        @Size(max = 10)
        String rowLabel,

        @NotNull
        SeatType seatType) {
}