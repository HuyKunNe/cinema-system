package com.cinema.user.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.user.exception.UserErrorCode;

@Component
public class EmailVerificationTokenCodec {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public EmailVerificationTokenCodec(
            SecureRandom secureRandom) {

        this.secureRandom = secureRandom;
    }

    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8));

            return HexFormat.of()
                    .formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new InternalServerException(
                    UserErrorCode.EMAIL_VERIFICATION_CRYPTO_FAILURE,
                    exception);
        }
    }
}
