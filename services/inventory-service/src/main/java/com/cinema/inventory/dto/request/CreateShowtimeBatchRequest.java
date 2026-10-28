package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateShowtimeBatchRequest(
        @NotNull
                @Schema(
                        description =
                                "Stable identifier for this batch creation attempt. "
                                        + "Reuse with identical items to retrieve the original "
                                        + "successful result. Use a new identifier when editing items.")
                UUID clientRequestId,
        @NotEmpty
                @Size(max = 50)
                @Schema(
                        description =
                                "Between 1 and 50 showtimes in request order. "
                                        + "Item order is part of the idempotency payload.")
                List<@NotNull @Valid CreateShowtimeRequest> showtimes) {

    public static final int MAX_SHOWTIMES = 50;

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(
                InventoryErrorCode.INVALID_SHOWTIME_BATCH);
    }
}
