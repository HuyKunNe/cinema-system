package com.cinema.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cinema.user.registration")
public record UserRegistrationProperties(
        String emailFrom, String verificationUrl, String pageUrl, Duration resendCooldown) {}
