package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse.HolidayDateResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        description =
                "Stored FARE_MATRIX_V2 configuration. "
                        + "Does not evaluate prices or validate publication readiness.")
public record CinemaFareMatrixResponse(
        CinemaPricingPolicyResponse policy,
        List<MatrixDayGroupResponse> dayGroups,
        List<MatrixTimeBandResponse> timeBands,
        List<MatrixFareRuleResponse> fareRules,
        List<HolidayDateResponse> holidayDates) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixDayGroupResponse(
            UUID id,
            String code,
            String name,
            List<DayOfWeek> daysOfWeek,
            boolean includesHolidays) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixTimeBandResponse(
            UUID id,
            String code,
            String name,
            @Schema(minimum = "0", maximum = "1439")
                    int fromMinute,
            @Schema(minimum = "1", maximum = "1440")
                    int toMinute) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatrixFareRuleResponse(
            UUID id,
            String code,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            String dayGroupCode,
            String timeBandCode,
            String ticketCategoryCode,
            String seatTypeCode,
            AdvanceScreeningCondition advanceScreeningCondition,
            @Schema(nullable = true)
                    String requiredEligibilityCode,
            @Schema(
                            type = "string",
                            pattern = "^(0|[1-9][0-9]*)$",
                            example = "150000",
                            description =
                                    "Non-negative integer VND amount serialized "
                                            + "as a decimal string to preserve precision.")
                    String amountVnd,
            FareUnit fareUnit,
            int priority,
            LocalDate effectiveFrom,
            @Schema(nullable = true)
                    LocalDate effectiveTo) {}
}
