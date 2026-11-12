package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateShowtimeBatchRequest(
        @NotNull UUID clientRequestId,
        @NotEmpty @Size(min = 1, max = MAX_SHOWTIMES)
                List<@NotNull @Valid CreateShowtimeRequest> showtimes) {

    public static final int MAX_SHOWTIMES = 50;

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_BATCH);
    }
}
