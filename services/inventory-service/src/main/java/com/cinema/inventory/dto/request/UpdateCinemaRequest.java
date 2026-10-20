package com.cinema.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCinemaRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @NotNull Boolean active,
        @Size(max = 64)
                @Schema(
                        nullable = true,
                        example = "Asia/Ho_Chi_Minh",
                        description =
                                "Region-based time zone supported by the server. "
                                        + "Omission or null preserves the current value. "
                                        + "Blank values are rejected.")
                String timeZone) {

    public UpdateCinemaRequest(
            String name,
            String address,
            String city,
            Boolean active) {

        this(name, address, city, active, null);
    }
}
