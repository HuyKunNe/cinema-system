package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.TicketCategory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Set;

public record PreviewDraftBasePriceRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest
                                        .StrictLongDeserializer.class)
                @Schema(description = "Expected policy.version, not policyVersion.")
                Long expectedVersion,
        @NotNull
                @Schema(
                        example = "2026-11-03T18:30:00+07:00",
                        description =
                                "Hypothetical showtime start with an ISO-8601 offset.")
                OffsetDateTime startsAt,
        @NotNull TicketCategory ticketCategory,
        @Size(max = 20)
                @Schema(
                        description =
                                "Hypothetical membership codes for simulation only. "
                                        + "Omission means no membership conditions are satisfied.")
                Set<@NotBlank @Size(max = 64) String> simulatedMemberTiers,
        @Size(max = 64)
                @Schema(
                        nullable = true,
                        description =
                                "Hypothetical sales channel code. "
                                        + "Omission matches only channel-unrestricted rules.")
                String simulatedChannel) {}
