package com.cinema.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

class SubjectValidatorTest {

    private final SubjectValidator validator = new SubjectValidator();

    @Test
    void userUuidSubjectShouldBeAccepted() {

        OAuth2TokenValidatorResult result =
                validator.validate(jwt("019c1234-1111-7abc-8def-0123456789ab"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void serviceClientSubjectShouldBeAccepted() {

        OAuth2TokenValidatorResult result = validator.validate(jwt("booking-service"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void missingSubjectShouldBeRejected() {

        OAuth2TokenValidatorResult result = validator.validate(jwt(null));

        assertInvalidSubject(result);
    }

    @Test
    void blankSubjectShouldBeRejected() {

        OAuth2TokenValidatorResult result = validator.validate(jwt("   "));

        assertInvalidSubject(result);
    }

    @Test
    void nullJwtShouldBeRejected() {

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jwt must not be null");
    }

    private void assertInvalidSubject(OAuth2TokenValidatorResult result) {

        assertThat(result.hasErrors()).isTrue();

        assertThat(result.getErrors())
                .singleElement()
                .satisfies(
                        error -> {
                            assertThat(error.getErrorCode()).isEqualTo("invalid_token");

                            assertThat(error.getDescription()).isEqualTo("JWT subject is required");
                        });
    }

    private Jwt jwt(String subject) {

        Instant now = Instant.now();

        Jwt.Builder builder =
                Jwt.withTokenValue("token")
                        .header("alg", "RS256")
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(300));

        if (subject != null) {
            builder.subject(subject);
        }

        return builder.build();
    }
}
