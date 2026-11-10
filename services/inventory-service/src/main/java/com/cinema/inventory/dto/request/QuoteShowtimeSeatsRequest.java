package com.cinema.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuoteShowtimeSeatsRequest(
        @NotEmpty @Size(max = 100) @Valid List<@NotNull SeatSelection> seats) {

    public record SeatSelection(
            @NotBlank @Size(max = 20) String seatNumber,
                    UUID ticketGroupId) {}
}
