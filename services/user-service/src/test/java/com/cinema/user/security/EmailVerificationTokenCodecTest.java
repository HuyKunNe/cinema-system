package com.cinema.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenCodecTest {

    @Mock
    private SecureRandom secureRandom;

    private EmailVerificationTokenCodec tokenCodec;

    @BeforeEach
    void setUp() {
        tokenCodec = new EmailVerificationTokenCodec(
                secureRandom);
    }

    @Test
    void generateRawTokenShouldCreateUrlSafeToken() {
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);

            for (int index = 0; index < bytes.length; index++) {

                bytes[index] = (byte) index;
            }

            return null;
        }).when(secureRandom).nextBytes(any());

        String token = tokenCodec.generateRawToken();

        assertThat(token)
                .hasSize(43)
                .matches("^[A-Za-z0-9_-]{43}$")
                .doesNotContain("=");

        byte[] decoded = Base64.getUrlDecoder()
                .decode(token);

        assertThat(decoded).hasSize(32);

        verify(secureRandom)
                .nextBytes(any(byte[].class));
    }

    @Test
    void hashShouldReturnLowercaseSha256Hex() {
        assertThat(tokenCodec.hash("abc"))
                .isEqualTo(
                        "ba7816bf8f01cfea"
                                + "414140de5dae2223"
                                + "b00361a396177a9c"
                                + "b410ff61f20015ad");
    }
}
