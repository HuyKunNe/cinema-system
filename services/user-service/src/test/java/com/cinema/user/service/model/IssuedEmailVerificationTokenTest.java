package com.cinema.user.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

public class IssuedEmailVerificationTokenTest {
    private static final String RAW_TOKEN = "A".repeat(43);

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse(
            "2026-08-08T03:00:00Z");

    @Test
    void shouldExposeIssuedTokenData() {
        IssuedEmailVerificationToken token = new IssuedEmailVerificationToken(
                RAW_TOKEN,
                EXPIRES_AT);

        assertThat(token.getRawToken())
                .isEqualTo(RAW_TOKEN);

        assertThat(token.getExpiresAt())
                .isEqualTo(EXPIRES_AT);
    }

    @Test
    void toStringShouldRedactRawToken() {
        IssuedEmailVerificationToken token = new IssuedEmailVerificationToken(
                RAW_TOKEN,
                EXPIRES_AT);

        assertThat(token.toString())
                .contains("[REDACTED]")
                .contains(EXPIRES_AT.toString())
                .doesNotContain(RAW_TOKEN);
    }
}
