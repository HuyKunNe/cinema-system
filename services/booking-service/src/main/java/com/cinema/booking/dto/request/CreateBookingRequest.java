package com.cinema.booking.dto.request;

import com.cinema.common.validation.annotation.UuidV5OrV7;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotBlank(message = "Client request ID is required") @Size(max = 100)
                String clientRequestId,
        @NotNull @UuidV5OrV7 UUID showtimeId,
        @NotEmpty @Valid List<@NotBlank @Size(max = 20) String> seatNumbers,
        @DecimalMin("0.00")
                @Digits(integer = 17, fraction = 2)
                @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        description =
                                "Expected total seat price in VND from the latest "
                                        + "seat quote. Inventory rejects the reservation "
                                        + "when the stored seat-price total differs.")
                BigDecimal expectedTotalAmount) {

    public CreateBookingRequest(String clientRequestId, UUID showtimeId, List<String> seatNumbers) {

        this(clientRequestId, showtimeId, seatNumbers, null);
    }
}
