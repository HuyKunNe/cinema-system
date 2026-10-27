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

public record CreateShowtimeBatchRequest(
        @NotEmpty
                @Size(max = 50)
                @Schema(
                        description =
                                "Showtimes to create atomically. "
                                        + "Contains between 1 and 50 items. "
                                        + "Response order matches request order.")
                List<@NotNull @Valid CreateShowtimeRequest> showtimes) {

    public static final int MAX_SHOWTIMES = 50;

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(
                InventoryErrorCode.INVALID_SHOWTIME_BATCH);
    }
}
