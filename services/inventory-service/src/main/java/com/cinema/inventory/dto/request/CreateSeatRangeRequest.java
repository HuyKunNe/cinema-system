package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeatRangeRequest(
        @NotBlank @Size(max = 10) String rowLabel,
        @NotNull @Min(1) @Max(999) Integer startNumber,
        @NotNull @Min(1) @Max(999) Integer endNumber,
        @NotNull SeatType seatType) {

    @JsonIgnore
    @AssertTrue(message = "endNumber must be greater than or equal to startNumber")
    public boolean isRangeValid() {
        return startNumber == null || endNumber == null || endNumber >= startNumber;
    }
}
