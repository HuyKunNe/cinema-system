package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReplacePricingConfigurationRequest(
        @NotNull @Min(0) Long expectedVersion,
        @NotEmpty List<@NotNull @Valid TimeBandInput> timeBands,
        @NotEmpty List<@NotNull @Valid TicketGroupInput> ticketGroups) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw invalid();
    }

    public record TimeBandInput(
            UUID id,
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(0) @Max(1439) Integer startMinute,
            @NotNull @Min(0) @Max(1440) Integer endMinute,
            @NotNull @Min(0) Integer sortOrder) {

        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object value) {
            throw invalid();
        }
    }

    public record TicketGroupInput(
            UUID id,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String eligibilityDescription,
            @NotNull Boolean defaultGroup,
            @NotNull @Min(0) Integer sortOrder) {

        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object value) {
            throw invalid();
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
    }
}
