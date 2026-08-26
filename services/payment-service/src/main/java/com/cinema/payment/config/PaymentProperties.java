package com.cinema.payment.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cinema.payment")
public record PaymentProperties(@NotBlank String provider) {}
