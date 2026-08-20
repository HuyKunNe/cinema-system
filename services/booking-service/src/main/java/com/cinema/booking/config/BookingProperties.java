package com.cinema.booking.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cinema.booking")
public record BookingProperties(
        @NotNull Duration reservationExpiration, @Positive int maxSeatsPerBooking) {}
