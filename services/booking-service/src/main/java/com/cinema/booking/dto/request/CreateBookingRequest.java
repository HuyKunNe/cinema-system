package com.cinema.booking.dto.request;

import com.cinema.common.validation.annotation.UuidV5OrV7;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Supply exactly one of seatNumbers or seats.")
public record CreateBookingRequest(
        @NotBlank @Size(max = 100) String clientRequestId,
        @NotNull @UuidV5OrV7 UUID showtimeId,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Legacy seat selection using default ticket groups. "
                                        + "Omit when supplying seats.")
                List<@NotBlank @Size(max = 20) String> seatNumbers,
        @DecimalMin("0.00")
                @Digits(integer = 17, fraction = 2)
                @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        description = "Expected total seat price in VND from the seat quote.")
                BigDecimal expectedTotalAmount,
        @Valid
                @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Seat selections with optional individual ticket groups. "
                                        + "Omit when supplying seatNumbers.")
                List<@NotNull BookingSeatSelectionRequest> seats) {

    public CreateBookingRequest(String clientRequestId, UUID showtimeId, List<String> seatNumbers) {

        this(clientRequestId, showtimeId, seatNumbers, null, null);
    }

    public CreateBookingRequest(
            String clientRequestId,
            UUID showtimeId,
            List<String> seatNumbers,
            BigDecimal expectedTotalAmount) {

        this(clientRequestId, showtimeId, seatNumbers, expectedTotalAmount, null);
    }

    @JsonIgnore
    @AssertTrue(message = "Supply exactly one non-empty seatNumbers or seats list")
    public boolean isSeatSelectionValid() {
        return (seatNumbers != null && !seatNumbers.isEmpty() && seats == null)
                || (seats != null && !seats.isEmpty() && seatNumbers == null);
    }
}
