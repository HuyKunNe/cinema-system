package com.cinema.user.oauth2.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;

@Component
public class Sha256RefreshTokenHasher
        implements RefreshTokenHasher {

    @Override
    public String hash(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            throw new ValidationException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_HASH_INVALID);
        }

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(rawToken.getBytes(
                            StandardCharsets.UTF_8));

            return HexFormat.of()
                    .formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new InternalServerException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_CRYPTO_FAILURE,
                    exception);
        }
    }
}
