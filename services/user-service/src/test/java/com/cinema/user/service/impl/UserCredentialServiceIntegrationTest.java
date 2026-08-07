package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserCredentialService;

@Transactional
class UserCredentialServiceIntegrationTest extends AbstractMySqlIntegrationTest {
    @Autowired
    private UserCredentialService userCredentialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    private static final String INITIAL_PASSWORD = "initial-password-123";

    private static final String NEW_PASSWORD = "new-secure-password-456";

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString();

        return userRepository.saveAndFlush(new User(
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix,
                prefix + "." + suffix));
    }

    @Test
    void createCredentialShouldPersistEncodedPassword() {
        User user = createUser("create");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        UserCredential credential = userCredentialRepository
                .findByUser_Id(user.getId())
                .orElseThrow();

        assertThat(credential.getPasswordHash())
                .startsWith("{bcrypt}")
                .doesNotContain(INITIAL_PASSWORD);

        assertThat(credential.getPasswordHashAlgorithm())
                .isEqualTo("bcrypt");

        assertThat(credential.getPasswordChangedAt())
                .isNotNull();

        assertThat(credential.getFailedAttemptCount())
                .isZero();

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                INITIAL_PASSWORD))
                .isTrue();
    }

    @Test
    void verifyPasswordShouldRejectIncorrectPassword() {
        User user = createUser("incorrect");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                "incorrect-password-999"))
                .isFalse();
    }

    @Test
    void changePasswordShouldReplaceExistingPassword() {
        User user = createUser("change");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        UserCredential before = userCredentialRepository
                .findByUser_Id(user.getId())
                .orElseThrow();

        String previousHash = before.getPasswordHash();
        OffsetDateTime previousChangedAt = before.getPasswordChangedAt();

        userCredentialService.changePassword(
                user.getId(),
                INITIAL_PASSWORD,
                NEW_PASSWORD);

        userCredentialRepository.flush();

        assertThat(before.getPasswordHash())
                .isNotEqualTo(previousHash)
                .startsWith("{bcrypt}");

        assertThat(before.getPasswordChangedAt())
                .isAfterOrEqualTo(previousChangedAt);

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                INITIAL_PASSWORD))
                .isFalse();

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                NEW_PASSWORD))
                .isTrue();
    }

    @Test
    void createCredentialShouldRejectDuplicateCredential() {
        User user = createUser("duplicate");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        assertThatThrownBy(() -> userCredentialService.createCredential(
                user.getId(),
                NEW_PASSWORD))
                .isInstanceOf(ConflictException.class);

        assertThat(userCredentialRepository
                .count())
                .isGreaterThanOrEqualTo(1L);
    }

    @Test
    void changePasswordShouldRejectIncorrectCurrentPassword() {
        User user = createUser("wrong-current");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        UserCredential credential = userCredentialRepository
                .findByUser_Id(user.getId())
                .orElseThrow();

        String originalHash = credential.getPasswordHash();

        assertThatThrownBy(() -> userCredentialService.changePassword(
                user.getId(),
                "incorrect-current-123",
                NEW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(credential.getPasswordHash())
                .isEqualTo(originalHash);

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                INITIAL_PASSWORD))
                .isTrue();
    }

    @Test
    void changePasswordShouldRejectSamePassword() {
        User user = createUser("same-password");

        userCredentialService.createCredential(
                user.getId(),
                INITIAL_PASSWORD);

        assertThatThrownBy(() -> userCredentialService.changePassword(
                user.getId(),
                INITIAL_PASSWORD,
                INITIAL_PASSWORD))
                .isInstanceOf(ValidationException.class);

        assertThat(userCredentialService.verifyPassword(
                user.getId(),
                INITIAL_PASSWORD))
                .isTrue();
    }
}
