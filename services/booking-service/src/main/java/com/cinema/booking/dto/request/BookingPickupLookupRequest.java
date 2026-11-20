package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BookingPickupLookupRequest(
        @NotBlank @Pattern(regexp = "^cinema-booking:v1:[A-Za-z0-9_-]{43}$") String qrPayload) {

    @Override
    public String toString() {
        return "BookingPickupLookupRequest[qrPayload=REDACTED]";
    }
}
