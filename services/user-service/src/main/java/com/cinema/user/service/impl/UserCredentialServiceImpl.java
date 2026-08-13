package com.cinema.user.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserCredentialService;

@Service
@Validated
@Transactional(readOnly = true)
public class UserCredentialServiceImpl implements UserCredentialService {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_BYTES = 72;
    private static final String PASSWORD_ALGORITHM = "bcrypt";

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    public UserCredentialServiceImpl(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            AuthorizationSessionRevocationService authorizationSessionRevocationService,
            PasswordEncoder passwordEncoder,
            Clock clock) {

        this.userRepository = userRepository;

        this.userCredentialRepository = userCredentialRepository;

        this.authorizationSessionRevocationService = authorizationSessionRevocationService;

        this.passwordEncoder = passwordEncoder;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void createCredential(
            UUID userId,
            String rawPassword) {

        validateNewPassword(rawPassword);

        User user = findUser(userId);

        if (userCredentialRepository.existsByUser_Id(userId)) {
            throw new ConflictException(
                    UserErrorCode.USER_CREDENTIAL_ALREADY_EXISTS);
        }

        UserCredential credential = new UserCredential(
                user,
                passwordEncoder.encode(rawPassword),
                PASSWORD_ALGORITHM,
                OffsetDateTime.now(clock));

        try {
            userCredentialRepository.saveAndFlush(credential);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    UserErrorCode.USER_CREDENTIAL_ALREADY_EXISTS,
                    exception);
        }
    }

    @Override
    @Transactional
    public void changePassword(
            UUID userId,
            String currentRawPassword,
            String newRawPassword) {

        validateNewPassword(newRawPassword);

        UserCredential credential = findCredential(userId);

        if (!matchesCandidate(
                currentRawPassword,
                credential.getPasswordHash())) {

            throw new UnauthorizedException(
                    UserErrorCode.INVALID_CREDENTIALS);
        }

        if (passwordEncoder.matches(
                newRawPassword,
                credential.getPasswordHash())) {

            throw new ValidationException(
                    UserErrorCode.PASSWORD_MUST_DIFFER);
        }

        credential.changePassword(
                passwordEncoder.encode(
                        newRawPassword),
                PASSWORD_ALGORITHM,
                OffsetDateTime.now(
                        clock));

        authorizationSessionRevocationService
                .revokeByPrincipalName(
                        credential
                                .getUser()
                                .getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(
            UUID userId,
            String newRawPassword) {

        validateNewPassword(
                newRawPassword);

        UserCredential credential = findCredential(
                userId);

        if (passwordEncoder.matches(
                newRawPassword,
                credential.getPasswordHash())) {

            throw new ValidationException(
                    UserErrorCode.PASSWORD_MUST_DIFFER);
        }

        credential.changePassword(
                passwordEncoder.encode(
                        newRawPassword),
                PASSWORD_ALGORITHM,
                OffsetDateTime.now(
                        clock));

        authorizationSessionRevocationService
                .revokeByPrincipalName(
                        credential
                                .getUser()
                                .getUsername());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        UserErrorCode.USER_NOT_FOUND));
    }

    private UserCredential findCredential(UUID userId) {
        return userCredentialRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException(
                        UserErrorCode.USER_CREDENTIAL_NOT_FOUND));
    }

    private static void validateNewPassword(
            String rawPassword) {

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException(
                    UserErrorCode.PASSWORD_REQUIRED);
        }

        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    UserErrorCode.PASSWORD_TOO_SHORT);
        }

        if (utf8Length(rawPassword) > MAX_BCRYPT_BYTES) {
            throw new ValidationException(
                    UserErrorCode.PASSWORD_TOO_LONG);
        }
    }

    private boolean matchesCandidate(
            String rawPassword,
            String encodedPassword) {

        if (rawPassword == null
                || rawPassword.isEmpty()
                || utf8Length(rawPassword) > MAX_BCRYPT_BYTES) {

            return false;
        }

        return passwordEncoder.matches(
                rawPassword,
                encodedPassword);
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    @Transactional
    public boolean verifyPassword(
            UUID userId,
            String rawPassword) {

        UserCredential credential = userCredentialRepository
                .findByUser_Id(userId)
                .orElse(null);

        if (credential == null
                || !matchesCandidate(
                        rawPassword,
                        credential.getPasswordHash())) {

            return false;
        }

        if (passwordEncoder.upgradeEncoding(
                credential.getPasswordHash())) {

            credential.changePassword(
                    passwordEncoder.encode(rawPassword),
                    PASSWORD_ALGORITHM,
                    OffsetDateTime.now(clock));
        }

        return true;
    }
}
