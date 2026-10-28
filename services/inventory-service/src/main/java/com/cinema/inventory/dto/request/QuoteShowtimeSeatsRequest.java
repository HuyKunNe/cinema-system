package com.cinema.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(requiredProperties = {"seats"})
public record QuoteShowtimeSeatsRequest(
        @NotEmpty @Size(max = 100) @Valid List<@NotNull SeatSelection> seats) {

    @Schema(requiredProperties = {"seatNumber"})
    public record SeatSelection(
            @NotBlank @Size(max = 20) String seatNumber,
            @Schema(
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                            nullable = true,
                            description =
                                    "Ticket group owned by the assigned pricing "
                                            + "version. Omit to use its default group.")
                    UUID ticketGroupId) {}
}
