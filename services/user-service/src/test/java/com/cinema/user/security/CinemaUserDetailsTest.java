package com.cinema.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.cinema.user.enums.AccountStatus;

public class CinemaUserDetailsTest {
    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final String USERNAME = "member";

    private static final String PASSWORD_HASH = "{bcrypt}$2a$10$encoded";

    private CinemaUserDetails createDetails(
            AccountStatus status) {

        return new CinemaUserDetails(
                USER_ID,
                USERNAME,
                PASSWORD_HASH,
                status,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority(
                                "booking:read")));
    }

    @Test
    void shouldExposePrincipalData() {
        CinemaUserDetails details = createDetails(AccountStatus.ACTIVE);

        assertThat(details.getUserId())
                .isEqualTo(USER_ID);

        assertThat(details.getUsername())
                .isEqualTo(USERNAME);

        assertThat(details.getPassword())
                .isEqualTo(PASSWORD_HASH);

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(
                        "ROLE_USER",
                        "booking:read");
    }

    @Test
    void shouldSortAndDeduplicateAuthorities() {
        GrantedAuthority blankAuthority = () -> " ";
        GrantedAuthority nullValueAuthority = () -> null;

        CinemaUserDetails details = new CinemaUserDetails(
                USER_ID,
                USERNAME,
                PASSWORD_HASH,
                AccountStatus.ACTIVE,
                Arrays.asList(
                        new SimpleGrantedAuthority(
                                "booking:read"),
                        new SimpleGrantedAuthority(
                                "ROLE_USER"),
                        new SimpleGrantedAuthority(
                                "booking:read"),
                        blankAuthority,
                        nullValueAuthority,
                        null));

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(
                        "ROLE_USER",
                        "booking:read");
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE, true, true",
            "PENDING_VERIFICATION, false, true",
            "LOCKED, false, false",
            "DISABLED, false, true"
    })
    void shouldMapAccountStatus(
            AccountStatus status,
            boolean enabled,
            boolean accountNonLocked) {

        CinemaUserDetails details = createDetails(status);

        assertThat(details.isEnabled())
                .isEqualTo(enabled);

        assertThat(details.isAccountNonLocked())
                .isEqualTo(accountNonLocked);

        assertThat(details.isAccountNonExpired())
                .isTrue();

        assertThat(details.isCredentialsNonExpired())
                .isTrue();
    }

    @Test
    void shouldErasePasswordHash() {
        CinemaUserDetails details = createDetails(AccountStatus.ACTIVE);

        details.eraseCredentials();

        assertThat(details.getPassword()).isNull();
    }

    @Test
    void principalsWithSameUserIdShouldBeEqualForSessionLookup() {
        CinemaUserDetails original = new CinemaUserDetails(
                USER_ID,
                "customer@example.com",
                "encoded-password",
                AccountStatus.ACTIVE,
                List.of());

        CinemaUserDetails deserialized = new CinemaUserDetails(
                USER_ID,
                "customer@example.com",
                null,
                AccountStatus.ACTIVE,
                List.of());

        assertThat(deserialized)
                .isEqualTo(
                        original);

        assertThat(deserialized.hashCode())
                .isEqualTo(
                        original.hashCode());
    }
}
