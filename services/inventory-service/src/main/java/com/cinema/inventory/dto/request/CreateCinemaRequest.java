package com.cinema.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCinemaRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 32) String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 150) String openingHours,
        @Size(max = 1000) String directions,
        @Size(max = 64) String timeZone) {

    public CreateCinemaRequest(
            String name,
            String address,
            String city) {

        this(name, address, city, null, null, null, null, null);
    }
}
