package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cinema.user.enums.AccountStatus;
import com.cinema.user.security.CinemaUserDetails;

class AuthenticationProviderConfigurationTest {

    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final String USERNAME = "member";

    private static final String RAW_PASSWORD = "correct-password-123";

    private PasswordEncoder passwordEncoder;

    private UserDetailsPasswordService userDetailsPasswordService;

    private AuthenticationProviderConfiguration configuration;

    @BeforeEach
    void setUp() {
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        userDetailsPasswordService = mock(UserDetailsPasswordService.class);

        configuration = new AuthenticationProviderConfiguration();
    }

    @Test
    void shouldAuthenticateActiveUser() {
        CinemaUserDetails userDetails = createUserDetails(AccountStatus.ACTIVE);

        AuthenticationManager manager = createManager(identifier -> userDetails);

        Authentication result = manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                USERNAME,
                                RAW_PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();

        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "booking:create");

        assertThat(result.getPrincipal()).isInstanceOf(CinemaUserDetails.class);

        CinemaUserDetails principal = (CinemaUserDetails) result.getPrincipal();

        assertThat(principal.getUserId()).isEqualTo(USER_ID);

        assertThat(result.getCredentials()).isNull();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void shouldRejectIncorrectPassword() {
        CinemaUserDetails userDetails = createUserDetails(AccountStatus.ACTIVE);

        AuthenticationManager manager = createManager(identifier -> userDetails);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                USERNAME,
                                "incorrect-password-999")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldHideUsernameNotFoundException() {
        UserDetailsService userDetailsService = identifier -> {
            throw new UsernameNotFoundException("Invalid credentials");
        };

        AuthenticationManager manager = createManager(userDetailsService);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                "missing",
                                RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectLockedUser() {
        CinemaUserDetails userDetails = createUserDetails(AccountStatus.LOCKED);

        AuthenticationManager manager = createManager(identifier -> userDetails);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                USERNAME,
                                RAW_PASSWORD)))
                .isInstanceOf(LockedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {
            "DISABLED",
            "PENDING_VERIFICATION"
    })
    void shouldRejectUserWhenAuthenticationIsDisabled(AccountStatus status) {

        CinemaUserDetails userDetails = createUserDetails(status);

        AuthenticationManager manager = createManager(identifier -> userDetails);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                USERNAME,
                                RAW_PASSWORD)))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void shouldPersistUpgradedPasswordHash() {
        PasswordEncoder upgradingEncoder = mock(PasswordEncoder.class);

        UserDetailsService userDetailsService = mock(UserDetailsService.class);

        UserDetailsPasswordService passwordService = mock(UserDetailsPasswordService.class);

        CinemaUserDetails currentDetails = new CinemaUserDetails(
                USER_ID,
                USERNAME,
                "{bcrypt}$2a$08$old",
                AccountStatus.ACTIVE,
                List.of());

        CinemaUserDetails updatedDetails = new CinemaUserDetails(
                USER_ID,
                USERNAME,
                "{bcrypt}$2a$10$new",
                AccountStatus.ACTIVE,
                List.of());

        when(userDetailsService
                .loadUserByUsername(USERNAME))
                .thenReturn(currentDetails);

        when(upgradingEncoder.matches(
                RAW_PASSWORD,
                "{bcrypt}$2a$08$old"))
                .thenReturn(true);

        when(upgradingEncoder.upgradeEncoding(
                "{bcrypt}$2a$08$old"))
                .thenReturn(true);

        when(upgradingEncoder.encode(RAW_PASSWORD))
                .thenReturn("{bcrypt}$2a$10$new");

        when(passwordService.updatePassword(
                currentDetails,
                "{bcrypt}$2a$10$new"))
                .thenReturn(updatedDetails);

        AuthenticationProvider provider = configuration
                .userAuthenticationProvider(
                        userDetailsService,
                        passwordService,
                        upgradingEncoder);

        AuthenticationManager manager = configuration.authenticationManager(provider);

        Authentication result = manager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                USERNAME,
                                RAW_PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();

        verify(passwordService).updatePassword(
                currentDetails,
                "{bcrypt}$2a$10$new");
    }

    private AuthenticationManager createManager(UserDetailsService userDetailsService) {

        AuthenticationProvider provider = configuration
                .userAuthenticationProvider(
                        userDetailsService,
                        userDetailsPasswordService,
                        passwordEncoder);

        return configuration.authenticationManager(provider);
    }

    private CinemaUserDetails createUserDetails(AccountStatus status) {

        return new CinemaUserDetails(
                USER_ID,
                USERNAME,
                passwordEncoder.encode(RAW_PASSWORD),
                status,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("booking:create")));
    }
}
