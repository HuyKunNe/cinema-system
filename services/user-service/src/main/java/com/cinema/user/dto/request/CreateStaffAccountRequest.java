package com.cinema.user.dto.request;

import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStaffAccountRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,100}") String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 32) String phoneNumber,
        @NotNull @UuidV7 UUID cinemaId) {

    @Override
    public String toString() {
        return "CreateStaffAccountRequest[REDACTED]";
    }
}
