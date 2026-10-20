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
                        nullable = true,
                        example = "Asia/Ho_Chi_Minh",
                        description =
                                "Region-based time zone supported by the server. Optional during"
                                    + " migration; omission leaves it unconfigured.")
                String timeZone) {

    public CreateCinemaRequest(String name, String address, String city) {
        this(name, address, city, null);
    }
}
