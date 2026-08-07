package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordSecurityConfigurationTest {

    private static final String RAW_PASSWORD = "correct-horse-battery-staple";

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        PasswordSecurityConfiguration configuration = new PasswordSecurityConfiguration();

        passwordEncoder = configuration.passwordEncoder();
    }

    @Test
    void shouldEncodePasswordUsingDelegatingFormat() {
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(encoded)
                .startsWith("{bcrypt}")
                .doesNotContain(RAW_PASSWORD);
    }

    @Test
    void shouldMatchCorrectPassword() {
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(passwordEncoder.matches(
                RAW_PASSWORD,
                encoded))
                .isTrue();
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(passwordEncoder.matches(
                "incorrect-password",
                encoded))
                .isFalse();
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String first = passwordEncoder.encode(RAW_PASSWORD);
        String second = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(first).isNotEqualTo(second);

        assertThat(passwordEncoder.matches(
                RAW_PASSWORD,
                first))
                .isTrue();

        assertThat(passwordEncoder.matches(
                RAW_PASSWORD,
                second))
                .isTrue();
    }
}
