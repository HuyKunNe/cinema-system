package com.cinema.common.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public final class SubjectValidator implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN = "invalid_token";

    private static final String INVALID_SUBJECT_DESCRIPTION = "JWT subject is required";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        Objects.requireNonNull(jwt, "jwt must not be null");

        String subject = jwt.getSubject();

        if (subject != null && !subject.isBlank()) {

            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(INVALID_TOKEN, INVALID_SUBJECT_DESCRIPTION, null);

        return OAuth2TokenValidatorResult.failure(error);
    }
}
