package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCinemaRequest(
        @NotBlank @Size(max = 150) String name,

        @NotBlank @Size(max = 500) String address,

        @NotBlank @Size(max = 100) String city) {
}