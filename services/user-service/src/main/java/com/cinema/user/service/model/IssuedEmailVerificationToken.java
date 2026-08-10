package com.cinema.user.service.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class IssuedEmailVerificationToken {

    private final String rawToken;
    private final OffsetDateTime expiresAt;

    public IssuedEmailVerificationToken(
            String rawToken,
            OffsetDateTime expiresAt) {

        this.rawToken = Objects.requireNonNull(rawToken);

        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public String getRawToken() {
        return rawToken;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "IssuedEmailVerificationToken"
                + "[rawToken=[REDACTED], expiresAt="
                + expiresAt
                + "]";
    }
}

