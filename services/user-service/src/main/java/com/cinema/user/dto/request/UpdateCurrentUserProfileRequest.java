package com.cinema.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters") String firstName,
        @Size(max = 100, message = "Last name must not exceed 100 characters") String lastName,
        @Size(max = 32, message = "Phone number must not exceed 32 characters")
                String phoneNumber) {}
