package com.cinema.inventory.dto.request;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingPolicyContentRequest.StrictIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckDraftFareMatrixCoverageRequest(
        @NotNull
                @Valid
                @Schema(
                        description =
                                "Fixed simulation conditions. startsAt selects the local date "
                                        + "in the policy time zone; its time does not set the window.")
                PreviewDraftFareMatrixRequest scenario,
        @NotNull
                @Min(0)
                @Max(1439)
                @JsonDeserialize(using = StrictIntegerDeserializer.class)
                Integer fromMinute,
        @NotNull
                @Min(1)
                @Max(1440)
                @JsonDeserialize(using = StrictIntegerDeserializer.class)
                Integer toMinute) {

    @JsonIgnore
    @AssertTrue(message = "toMinute must be after fromMinute")
    public boolean isWindowValid() {
        return fromMinute == null
                || toMinute == null
                || toMinute > fromMinute;
    }
}
