package com.cinema.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.repository.UserCredentialRepository;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsPasswordServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-07T03:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    @Mock
    private UserCredentialRepository userCredentialRepository;

    private JpaUserDetailsPasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new JpaUserDetailsPasswordService(
                userCredentialRepository,
                FIXED_CLOCK);
    }

    @Test
    void shouldUpdateEncodedPassword() {
        User user = mock(User.class);

        when(user.getStatus())
                .thenReturn(AccountStatus.ACTIVE);

        UserCredential credential = new UserCredential(
                user,
                "{bcrypt}$2a$08$old",
                "bcrypt",
                FIXED_TIME.minusDays(1));

        CinemaUserDetails principal = new CinemaUserDetails(
                USER_ID,
                "member",
                "{bcrypt}$2a$08$old",
                AccountStatus.ACTIVE,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER")));

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        UserDetails result = passwordService.updatePassword(
                principal,
                "{bcrypt}$2a$10$new");

        assertThat(credential.getPasswordHash())
                .isEqualTo("{bcrypt}$2a$10$new");

        assertThat(
                credential.getPasswordHashAlgorithm())
                .isEqualTo("bcrypt");

        assertThat(credential.getPasswordChangedAt())
                .isEqualTo(FIXED_TIME);

        assertThat(result.getPassword())
                .isEqualTo("{bcrypt}$2a$10$new");

        assertThat(result.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        verify(userCredentialRepository)
                .findByUser_Id(USER_ID);

        verify(userCredentialRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectUnsupportedPrincipalType() {
        UserDetails unsupported = mock(UserDetails.class);

        assertThatThrownBy(() -> passwordService.updatePassword(
                unsupported,
                "{bcrypt}$2a$10$new"))
                .isInstanceOf(
                        InternalAuthenticationServiceException.class);

        verifyNoInteractions(
                userCredentialRepository);
    }

    @Test
    void shouldFailWhenCredentialDoesNotExist() {
        CinemaUserDetails principal = new CinemaUserDetails(
                USER_ID,
                "member",
                "{bcrypt}$2a$08$old",
                AccountStatus.ACTIVE,
                List.of());

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.updatePassword(
                principal,
                "{bcrypt}$2a$10$new"))
                .isInstanceOf(
                        InternalAuthenticationServiceException.class);

        verify(userCredentialRepository)
                .findByUser_Id(USER_ID);
    }
}
