package com.cinema.user.oauth2.model;

public record RegisteredClientRegistrationResult(
        String id,
        String clientId,
        String clientName) {
}
