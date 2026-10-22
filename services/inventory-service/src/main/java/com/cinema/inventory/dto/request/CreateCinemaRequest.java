package com.cinema.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCinemaRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 64)
                @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        defaultValue = "Asia/Ho_Chi_Minh",
                        example = "Asia/Ho_Chi_Minh",
                        description =
                                "Region-based time zone supported by the server. "
                                        + "Omission or null defaults to Asia/Ho_Chi_Minh "
                                        + "when creating a cinema. "
                                        + "Blank or unsupported values are rejected.")
                String timeZone) {

    public CreateCinemaRequest(
            String name,
            String address,
            String city) {

        this(name, address, city, null);
    }
}
