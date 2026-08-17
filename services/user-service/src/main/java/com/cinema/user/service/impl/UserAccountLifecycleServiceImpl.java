package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserAccountLifecycleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class UserAccountLifecycleServiceImpl implements UserAccountLifecycleService {

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private final Clock clock;

    public UserAccountLifecycleServiceImpl(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            AuthorizationSessionRevocationService authorizationSessionRevocationService,
            Clock clock) {

        this.userRepository = userRepository;

        this.userCredentialRepository = userCredentialRepository;

        this.authorizationSessionRevocationService = authorizationSessionRevocationService;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void verifyEmail(UUID userId) {

        User user = findUser(userId);

        user.verifyEmail(OffsetDateTime.now(clock));
    }

    @Override
    @Transactional
    public void lock(UUID userId) {

        User user = findUser(userId);

        user.lock(OffsetDateTime.now(clock));

        authorizationSessionRevocationService.revokeByPrincipalName(
                user.getUsername(), RevocationReason.ACCOUNT_LOCKED);
    }

    @Override
    @Transactional
    public void unlock(UUID userId) {

        User user = findUser(userId);

        user.unlock();
    }

    @Override
    @Transactional
    public void disable(UUID userId) {

        User user = findUser(userId);

        user.disable(OffsetDateTime.now(clock));

        authorizationSessionRevocationService.revokeByPrincipalName(
                user.getUsername(), RevocationReason.ACCOUNT_DISABLED);
    }

    @Override
    @Transactional
    public void enable(UUID userId) {

        User user = findUser(userId);

        user.enable();
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(UUID userId) {

        User user = findUser(userId);

        UserCredential credential =
                userCredentialRepository
                        .findByUser_Id(userId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                UserErrorCode.USER_CREDENTIAL_NOT_FOUND));

        user.recordSuccessfulLogin(OffsetDateTime.now(clock));

        credential.clearFailedAttempts();
    }

    private User findUser(UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }
}
