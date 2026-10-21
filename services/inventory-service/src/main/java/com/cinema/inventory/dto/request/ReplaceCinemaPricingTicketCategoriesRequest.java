package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.TicketEligibilityMode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceCinemaPricingTicketCategoriesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                @Schema(description = "Expected policy.version, not policyVersion.")
                Long expectedVersion,
        @NotNull
                @Size(max = 100)
                List<@NotNull @Valid CategoryInput> ticketCategories) {

    public record CategoryInput(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull TicketEligibilityMode eligibilityMode,
            @Size(max = 64)
                    @Schema(
                            nullable = true,
                            description =
                                    "Required only for RULE_REQUIRED. "
                                            + "Declaring a code does not verify its implementation.")
                    String eligibilityRuleCode) {}
}
