package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareSurchargeUnit;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ReplaceCinemaFareSurchargesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull FareSurchargeMode mode,
        @NotNull
                @Size(max = 5000)
                List<@NotNull @Valid FareSurchargeInput> surcharges) {

    public record FareSurchargeInput(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 64) String fareRuleCode,
            @Size(max = 64) String movieAttributeCode,
            @Size(max = 64) String requiredEligibilityCode,
            @NotBlank
                    @Pattern(regexp = "0|[1-9][0-9]{0,18}")
                    @JsonDeserialize(
                            using =
                                    ReplaceCinemaFareMatrixRequest
                                            .StrictAmountStringDeserializer.class)
                    @Schema(
                            type = "string",
                            description =
                                    "Non-negative integer VND string. "
                                            + "Maximum 9223372036854775807.")
                    String amountVnd,
            @NotNull FareSurchargeUnit unit,
            @NotNull
                    @PositiveOrZero
                    @JsonDeserialize(
                            using =
                                    ReplaceCinemaPricingPolicyContentRequest
                                            .StrictIntegerDeserializer.class)
                    Integer priority,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo) {}
}
