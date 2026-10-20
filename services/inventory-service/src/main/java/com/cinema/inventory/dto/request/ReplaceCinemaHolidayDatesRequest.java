package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ReplaceCinemaHolidayDatesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull
                @Size(max = 1000)
                List<@NotNull @Valid HolidayDateInput> holidayDates) {

    public record HolidayDateInput(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 150) String name) {}
}
