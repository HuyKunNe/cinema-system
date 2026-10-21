package com.cinema.inventory.dto.request;

import java.time.LocalDate;

import com.cinema.inventory.enums.PricingPolicyModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
                        description = "Exclusive ending local date. Null means no ending date.")
                LocalDate effectiveTo,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        defaultValue = "BASE_RULES_V1",
                        description =
                                "Immutable pricing model. Omission or null retains "
                                        + "BASE_RULES_V1 compatibility. FARE_MATRIX_V2 "
                                        + "creates metadata only until its rule APIs are implemented.")
                PricingPolicyModel pricingModel) {

    public CreateCinemaPricingPolicyRequest {
        if (pricingModel == null) {
            pricingModel = PricingPolicyModel.BASE_RULES_V1;
        }
    }

    public CreateCinemaPricingPolicyRequest(
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        this(
                currency,
                effectiveFrom,
                effectiveTo,
                PricingPolicyModel.BASE_RULES_V1);
    }

    @JsonIgnore
    @AssertTrue(message = "effectiveTo must be after effectiveFrom")
    public boolean isPeriodValid() {
        return effectiveFrom == null
                || effectiveTo == null
                || effectiveTo.isAfter(effectiveFrom);
    }
}
