package com.cinema.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.security.config.ReactiveSecurityConfiguration;
import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.properties.SecurityProperties;
import com.cinema.common.test.security.TestJwtIssuer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.List;

class JwtTrustValidationTest {

    private static final String ISSUER = "https://identity.jwt-validation.test";

    private static final String AUDIENCE = "cinema-api";

    private static final String SUBJECT = "019c1234-1111-7abc-8def-0123456789ab";

    private static final TestJwtIssuer TEST_ISSUER = new TestJwtIssuer(ISSUER, AUDIENCE);

    private JwtDecoder servletDecoder;

    private ReactiveJwtDecoder reactiveDecoder;

    @BeforeEach
    void setUp() {

        SecurityProperties properties =
                new SecurityProperties(
                        TEST_ISSUER.issuer(), TEST_ISSUER.jwkSetUri(), TEST_ISSUER.audience());

        AudienceValidator audienceValidator = new AudienceValidator(TEST_ISSUER.audience());

        SubjectValidator subjectValidator = new SubjectValidator();

        servletDecoder =
                new SecurityConfiguration()
                        .cinemaJwtDecoder(properties, audienceValidator, subjectValidator);

        reactiveDecoder =
                new ReactiveSecurityConfiguration()
                        .cinemaReactiveJwtDecoder(properties, audienceValidator, subjectValidator);
    }

    @AfterAll
    static void stopTestIssuer() {

        TEST_ISSUER.close();
    }

    @Test
    void validJwtShouldBeAcceptedByServletAndReactiveDecoders() {

        String token =
                TEST_ISSUER.token(
                        claims ->
                                claims.subject(SUBJECT)
                                        .claim("roles", List.of("USER"))
                                        .claim("permissions", List.of("booking:read")));

        Jwt servletJwt = servletDecoder.decode(token);

        Jwt reactiveJwt = reactiveDecoder.decode(token).block();

        assertThat(servletJwt).isNotNull();

        assertThat(reactiveJwt).isNotNull();

        assertThat(servletJwt.getSubject()).isEqualTo(SUBJECT);

        assertThat(reactiveJwt.getSubject()).isEqualTo(SUBJECT);

        assertThat(servletJwt.getAudience()).containsExactly(AUDIENCE);

        assertThat(reactiveJwt.getAudience()).containsExactly(AUDIENCE);
    }

    @Test
    void jwtWithUntrustedSignatureShouldBeRejected() {

        String token = TEST_ISSUER.untrustedToken(claims -> claims.subject(SUBJECT));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtWithWrongIssuerShouldBeRejected() {

        String token =
                TEST_ISSUER.token(
                        claims ->
                                claims.subject(SUBJECT)
                                        .issuer("https://untrusted.jwt-validation.test"));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtWithWrongAudienceShouldBeRejected() {

        String token =
                TEST_ISSUER.token(claims -> claims.subject(SUBJECT).audience("untrusted-api"));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtWithoutRequiredAudienceShouldBeRejected() {

        String token = TEST_ISSUER.token(claims -> claims.subject(SUBJECT).audience(List.of()));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void expiredJwtShouldBeRejected() {

        Instant now = Instant.now();

        String token =
                TEST_ISSUER.token(
                        claims ->
                                claims.subject(SUBJECT)
                                        .issueTime(Date.from(now.minusSeconds(600)))
                                        .notBeforeTime(Date.from(now.minusSeconds(600)))
                                        .expirationTime(Date.from(now.minusSeconds(300))));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtBeforeNotBeforeTimeShouldBeRejected() {

        Instant now = Instant.now();

        String token =
                TEST_ISSUER.token(
                        claims ->
                                claims.subject(SUBJECT)
                                        .notBeforeTime(Date.from(now.plusSeconds(300)))
                                        .expirationTime(Date.from(now.plusSeconds(600))));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtWithoutSubjectShouldBeRejected() {

        String token = TEST_ISSUER.token(claims -> claims.subject(null));

        assertRejectedByBothDecoders(token);
    }

    @Test
    void jwtWithBlankSubjectShouldBeRejected() {

        String token = TEST_ISSUER.token(claims -> claims.subject("   "));

        assertRejectedByBothDecoders(token);
    }

    private void assertRejectedByBothDecoders(String token) {

        assertThatThrownBy(() -> servletDecoder.decode(token)).isInstanceOf(JwtException.class);

        assertThatThrownBy(() -> reactiveDecoder.decode(token).block())
                .isInstanceOf(JwtException.class);
    }
}
