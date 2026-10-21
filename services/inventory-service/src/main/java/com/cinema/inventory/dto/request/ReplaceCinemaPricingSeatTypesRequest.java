package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceCinemaPricingSeatTypesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest.StrictLongDeserializer
                                        .class)
                @Schema(description = "Expected policy.version, not policyVersion.")
                Long expectedVersion,
        @NotNull @Size(max = 100) List<@NotNull @Valid SeatTypeInput> seatTypes) {

    public record SeatTypeInput(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull PricingSeatMappingStatus mappingStatus,
            @Schema(
                            nullable = true,
                            description =
                                    "Required for CONFIRMED; must be null for UNCONFIRMED. Mapping"
                                        + " does not determine fare units or surcharges.")
                    SeatType physicalSeatType) {}
}
