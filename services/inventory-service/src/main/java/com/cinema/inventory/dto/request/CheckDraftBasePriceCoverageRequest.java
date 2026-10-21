package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.TicketCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Set;

public record CheckDraftBasePriceCoverageRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull OffsetDateTime startsAt,
        @NotNull TicketCategory ticketCategory,
        @NotNull
                @Min(0)
                @Max(1439)
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictIntegerDeserializer.class)
                Integer fromMinute,
        @NotNull
                @Min(1)
                @Max(1440)
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictIntegerDeserializer.class)
                Integer toMinute,
        @Size(max = 20)
                Set<@NotBlank @Size(max = 64) String> simulatedMemberTiers,
        @Size(max = 64) String simulatedChannel) {

    @JsonIgnore
    @AssertTrue(message = "toMinute must be after fromMinute")
    public boolean isWindowValid() {
        return fromMinute == null
                || toMinute == null
                || toMinute > fromMinute;
    }
}
