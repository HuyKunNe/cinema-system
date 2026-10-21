package com.cinema.inventory.dto.request;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingPolicyContentRequest.StrictIntegerDeserializer;
import com.cinema.inventory.dto.request.ReplaceCinemaPricingPolicyContentRequest.StrictLongDeserializer;
import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReplaceCinemaFareMatrixRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull @Size(max = 100) List<@NotNull @Valid DayGroupInput> dayGroups,
        @NotNull @Size(max = 100) List<@NotNull @Valid TimeBandInput> timeBands,
        @NotNull @Size(max = 2000) List<@NotNull @Valid FareRuleInput> fareRules) {

    public record DayGroupInput(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull @Size(max = 7) Set<@NotNull DayOfWeek> daysOfWeek,
            @NotNull Boolean includesHolidays) {}

    public record TimeBandInput(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull @Min(0) @Max(1439) @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer fromMinute,
            @NotNull @Min(1) @Max(1440) @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer toMinute) {}

    public record FareRuleInput(
            @NotBlank @Size(max = 64) String code,
            @NotNull ExperienceType experienceType,
            @NotNull ContentFormat contentFormat,
            @NotBlank @Size(max = 64) String dayGroupCode,
            @NotBlank @Size(max = 64) String timeBandCode,
            @NotBlank @Size(max = 64) String ticketCategoryCode,
            @NotBlank @Size(max = 64) String seatTypeCode,
            @NotNull
                    @Schema(
                            description =
                                    "Explicit fare applicability: REGULAR_SCREENING or "
                                            + "ADVANCE_SCREENING. ANY is rejected for fare rules.")
                    AdvanceScreeningCondition advanceScreeningCondition,
            @Size(max = 64) String requiredEligibilityCode,
            @NotBlank
                    @Pattern(regexp = "0|[1-9][0-9]{0,18}")
                    @JsonDeserialize(using = StrictAmountStringDeserializer.class)
                    @Schema(
                            type = "string",
                            description =
                                    "Canonical non-negative integer VND string. "
                                            + "Maximum value: 9223372036854775807.",
                            example = "150000")
                    String amountVnd,
            @NotNull FareUnit fareUnit,
            @NotNull @PositiveOrZero @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer priority,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo) {}

    public static final class StrictAmountStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {

            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(
                        String.class, "amountVnd must be a JSON string containing integer VND");
            }

            return parser.getText();
        }
    }
}
