package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID showtimeId,
        String clientRequestId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt,
        String rejectionReason,
        List<BookingSeatResponse> seats,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    @JsonProperty("seatCount")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description =
                    "Number of selected inventory seats. "
                            + "A COUPLE seat counts as one inventory seat. "
                            + "Null when the seat list is unavailable.")
    public Integer getSeatCount() {

        return seats == null ? null : seats.size();
    }

    @JsonProperty("capacity")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description =
                    "Total number of people accommodated by the selected seats. "
                            + "Each COUPLE seat contributes two people. "
                            + "Null when any seat-type snapshot is unavailable or unsupported.")
    public Integer getCapacity() {

        if (seats == null) {
            return null;
        }

        int totalCapacity = 0;

        for (BookingSeatResponse seat : seats) {
            if (seat == null) {
                return null;
            }

            Integer seatCapacity = seat.getCapacity();

            if (seatCapacity == null) {
                return null;
            }

            totalCapacity += seatCapacity;
        }

        return totalCapacity;
    }
}
