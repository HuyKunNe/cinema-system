package com.cinema.common.security.jwt;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.security.error.SecurityErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceValidatorTest {

    private static final String REQUIRED_AUDIENCE = "cinema-api";

    private final AudienceValidator validator = new AudienceValidator(REQUIRED_AUDIENCE);

    @Test
    void shouldAcceptTokenContainingRequiredAudience() {
        Jwt jwt = jwt(List.of(
                "cinema-api",
                "another-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldRejectTokenWithIncorrectAudience() {
        Jwt jwt = jwt(List.of("another-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .containsExactly("invalid_token");
    }

    @Test
    void shouldRejectTokenWithoutAudience() {
        Jwt jwt = jwtWithoutAudience();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("description")
                .containsExactly(
                        "JWT does not contain the required audience");
    }

    @Test
    void shouldRejectBlankRequiredAudience() {
        assertThatThrownBy(() -> new AudienceValidator(" "))
                .isInstanceOf(InternalServerException.class)
                .satisfies(exception -> {
                    InternalServerException actual = (InternalServerException) exception;

                    assertThat(actual.getErrorCode())
                            .isEqualTo(
                                    SecurityErrorCode.INVALID_AUDIENCE_CONFIGURATION);
                });
    }

    @Test
    void shouldRejectNullRequiredAudience() {
        assertThatThrownBy(() -> new AudienceValidator(null))
                .isInstanceOf(InternalServerException.class)
                .satisfies(exception -> {
                    InternalServerException actual = (InternalServerException) exception;

                    assertThat(actual.getErrorCode())
                            .isEqualTo(
                                    SecurityErrorCode.INVALID_AUDIENCE_CONFIGURATION);
                });
    }

    @Test
    void shouldTrimConfiguredAudience() {
        AudienceValidator trimmingValidator = new AudienceValidator(" cinema-api ");

        OAuth2TokenValidatorResult result = trimmingValidator.validate(
                jwt(List.of("cinema-api")));

        assertThat(result.hasErrors()).isFalse();
    }

    private Jwt jwt(List<String> audiences) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(
                        "019c1234-1111-7abc-8def-0123456789ab")
                .audience(audiences)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }

    private Jwt jwtWithoutAudience() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(
                        "019c1234-1111-7abc-8def-0123456789ab")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }
}
