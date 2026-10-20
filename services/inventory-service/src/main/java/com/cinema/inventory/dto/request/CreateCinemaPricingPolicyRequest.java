package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCinemaPricingPolicyRequest(
        @NotBlank
                @Size(min = 3, max = 3)
                @Schema(example = "VND")
                String currency,
        @NotNull
                @Schema(
                        example = "2026-11-01",
                        description = "Inclusive first local date in the policy time zone.")
                LocalDate effectiveFrom,
        @Schema(
                        nullable = true,
                        example = "2026-12-01",
                        description =
                                "Exclusive ending local date. Null means no ending date.")
                LocalDate effectiveTo) {

    @JsonIgnore
    @AssertTrue(message = "effectiveTo must be after effectiveFrom")
    public boolean isPeriodValid() {
        return effectiveFrom == null
                || effectiveTo == null
                || effectiveTo.isAfter(effectiveFrom);
    }
}
