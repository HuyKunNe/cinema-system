package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserAccountLifecycleServiceImplTest {
    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final String USERNAME = "member@example.com";

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-07T03:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private User user;

    @Mock
    private UserCredential credential;

    @Mock
    private AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private UserAccountLifecycleServiceImpl lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new UserAccountLifecycleServiceImpl(
                userRepository,
                userCredentialRepository,
                authorizationSessionRevocationService,
                FIXED_CLOCK);
    }

    private void userExists() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
    }

    @Test
    void verifyEmailShouldUseServerTime() {
        userExists();

        lifecycleService.verifyEmail(USER_ID);

        verify(user).verifyEmail(FIXED_TIME);

        verify(userRepository)
                .findById(USER_ID);

        verifyNoInteractions(
                userCredentialRepository);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void lockShouldUseServerTimeAndRevokeAuthorizations() {
        userExists();

        when(user.getUsername())
                .thenReturn(
                        USERNAME);

        lifecycleService.lock(
                USER_ID);

        InOrder order = inOrder(
                user,
                authorizationSessionRevocationService);

        order.verify(user)
                .lock(
                        FIXED_TIME);

        order.verify(authorizationSessionRevocationService)
                .revokeByPrincipalName(
                        USERNAME,
                        RevocationReason.ACCOUNT_LOCKED);

        verifyNoInteractions(
                userCredentialRepository);
    }

    @Test
    void unlockShouldDelegateToUser() {
        userExists();

        lifecycleService.unlock(USER_ID);

        verify(user).unlock();

        verifyNoInteractions(
                userCredentialRepository);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void disableShouldUseServerTimeAndRevokeAuthorizations() {
        userExists();

        when(user.getUsername())
                .thenReturn(
                        USERNAME);

        lifecycleService.disable(
                USER_ID);

        InOrder order = inOrder(
                user,
                authorizationSessionRevocationService);

        order.verify(user)
                .disable(
                        FIXED_TIME);

        order.verify(authorizationSessionRevocationService)
                .revokeByPrincipalName(
                        USERNAME,
                        RevocationReason.ACCOUNT_DISABLED);

        verifyNoInteractions(
                userCredentialRepository);
    }

    @Test
    void enableShouldDelegateToUser() {
        userExists();

        lifecycleService.enable(USER_ID);

        verify(user).enable();

        verifyNoInteractions(
                userCredentialRepository);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void recordSuccessfulLoginShouldUpdateUserAndCredential() {
        userExists();

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        lifecycleService.recordSuccessfulLogin(
                USER_ID);

        InOrder order = inOrder(
                userRepository,
                userCredentialRepository,
                user,
                credential);

        order.verify(userRepository)
                .findById(USER_ID);

        order.verify(userCredentialRepository)
                .findByUser_Id(USER_ID);

        order.verify(user)
                .recordSuccessfulLogin(FIXED_TIME);

        order.verify(credential)
                .clearFailedAttempts();

        verify(userRepository, never())
                .save(any());

        verify(userCredentialRepository, never())
                .save(any());
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    static Stream<Consumer<UserAccountLifecycleServiceImpl>> operationsRequiringUser() {

        return Stream.of(
                service -> service.verifyEmail(USER_ID),
                service -> service.lock(USER_ID),
                service -> service.unlock(USER_ID),
                service -> service.disable(USER_ID),
                service -> service.enable(USER_ID),
                service -> service.recordSuccessfulLogin(
                        USER_ID));
    }

    @ParameterizedTest
    @MethodSource("operationsRequiringUser")
    void operationShouldThrowWhenUserDoesNotExist(
            Consumer<UserAccountLifecycleServiceImpl> operation) {

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(lifecycleService))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(
                userCredentialRepository,
                authorizationSessionRevocationService);
    }

    @Test
    void recordSuccessfulLoginShouldFailBeforeMutatingUserWhenCredentialIsMissing() {
        userExists();

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lifecycleService
                .recordSuccessfulLogin(USER_ID))
                .isInstanceOf(NotFoundException.class);

        verify(user, never())
                .recordSuccessfulLogin(any());

        verifyNoInteractions(credential);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }
}
