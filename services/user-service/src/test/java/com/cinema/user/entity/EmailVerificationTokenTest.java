package com.cinema.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;

public class EmailVerificationTokenTest {
    private static final String TOKEN_HASH = "a".repeat(64);

    private static final OffsetDateTime NOW = OffsetDateTime.parse(
            "2026-08-07T03:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = NOW.plusHours(1);

    private User createUser() {
        return new User(
                "member@example.com",
                "member@example.com",
                "member",
                "member");
    }

    private EmailVerificationToken createToken() {
        return new EmailVerificationToken(
                createUser(),
                TOKEN_HASH,
                EXPIRES_AT);
    }

    @Test
    void shouldCreateVerificationToken() {
        User user = createUser();

        EmailVerificationToken token = new EmailVerificationToken(
                user,
                TOKEN_HASH,
                EXPIRES_AT);

        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash())
                .isEqualTo(TOKEN_HASH);
        assertThat(token.getExpiresAt())
                .isEqualTo(EXPIRES_AT);
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getRevokedAt()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "short",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"
    })
    void shouldRejectInvalidTokenHash(
            String tokenHash) {

        assertThatThrownBy(() -> new EmailVerificationToken(
                createUser(),
                tokenHash,
                EXPIRES_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void shouldRequireExpiration() {
        assertThatThrownBy(() -> new EmailVerificationToken(
                createUser(),
                TOKEN_HASH,
                null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldBeUsableBeforeExpiration() {
        EmailVerificationToken token = createToken();

        assertThat(token.isUsableAt(NOW)).isTrue();
        assertThat(token.isUsableAt(
                EXPIRES_AT.minusNanos(1)))
                .isTrue();
    }

    @Test
    void shouldBeExpiredAtExpirationBoundary() {
        EmailVerificationToken token = createToken();

        assertThat(token.isUsableAt(EXPIRES_AT))
                .isFalse();

        assertThat(token.isUsableAt(
                EXPIRES_AT.plusNanos(1)))
                .isFalse();
    }

    @Test
    void markUsedShouldConsumeToken() {
        EmailVerificationToken token = createToken();

        token.markUsed(NOW);

        assertThat(token.getUsedAt())
                .isEqualTo(NOW);

        assertThat(token.isUsableAt(
                NOW.plusMinutes(1)))
                .isFalse();
    }

    @Test
    void markUsedShouldRejectExpiredToken() {
        EmailVerificationToken token = createToken();

        assertThatThrownBy(() -> token.markUsed(EXPIRES_AT))
                .isInstanceOf(ConflictException.class);

        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void markUsedShouldRejectAlreadyUsedToken() {
        EmailVerificationToken token = createToken();

        token.markUsed(NOW);

        assertThatThrownBy(() -> token.markUsed(
                NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(token.getUsedAt())
                .isEqualTo(NOW);
    }

    @Test
    void revokeShouldMakeTokenUnusable() {
        EmailVerificationToken token = createToken();

        token.revoke(NOW);

        assertThat(token.getRevokedAt())
                .isEqualTo(NOW);

        assertThat(token.isUsableAt(
                NOW.plusMinutes(1)))
                .isFalse();
    }

    @Test
    void usedTokenShouldNotBeRevoked() {
        EmailVerificationToken token = createToken();

        token.markUsed(NOW);

        assertThatThrownBy(() -> token.revoke(
                NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void revokedTokenShouldNotBeUsed() {
        EmailVerificationToken token = createToken();

        token.revoke(NOW);

        assertThatThrownBy(() -> token.markUsed(
                NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void operationsShouldRequireTimestamp() {
        EmailVerificationToken token = createToken();

        assertThatThrownBy(() -> token.isUsableAt(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThatThrownBy(() -> token.markUsed(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThatThrownBy(() -> token.revoke(null))
                .isInstanceOf(
                        ValidationException.class);
    }
}
