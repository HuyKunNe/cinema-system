package com.cinema.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaHolidayDatesResponse(
        CinemaPricingPolicyResponse policy,
        List<HolidayDateResponse> holidayDates) {

    public record HolidayDateResponse(
            UUID id,
            LocalDate date,
            String name) {}
}
