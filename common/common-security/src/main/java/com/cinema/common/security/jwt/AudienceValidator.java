package com.cinema.common.security.jwt;

import java.util.List;
import java.util.Objects;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.security.error.SecurityErrorCode;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AudienceValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN_ERROR_CODE = "invalid_token";

    private static final String INVALID_AUDIENCE_DESCRIPTION = "JWT does not contain the required audience";

    private final String requiredAudience;

    public AudienceValidator(String requiredAudience) {
        if (requiredAudience == null
                || requiredAudience.isBlank()) {
            throw new InternalServerException(
                    SecurityErrorCode.INVALID_AUDIENCE_CONFIGURATION);
        }

        this.requiredAudience = requiredAudience.trim();
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");

        List<String> audiences = jwt.getAudience();

        if (audiences != null
                && audiences.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                INVALID_TOKEN_ERROR_CODE,
                INVALID_AUDIENCE_DESCRIPTION,
                null);

        return OAuth2TokenValidatorResult.failure(error);
    }
}
