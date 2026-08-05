package com.cinema.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.enums.AccountStatus;

@Transactional
class UserRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Test
    void shouldPersistUserProfileAndCredential() {
        User user = userRepository.saveAndFlush(
                createUser(
                        "Alice@example.com",
                        "Alice"));

        UserProfile profile = userProfileRepository.saveAndFlush(
                new UserProfile(
                        user,
                        "Alice",
                        "Nguyen",
                        "0900000001"));

        UserCredential credential =
                userCredentialRepository.saveAndFlush(
                        new UserCredential(
                                user,
                                "{bcrypt}test-hash",
                                "bcrypt",
                                now()));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getId().version()).isEqualTo(7);
        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(credential.getId()).isNotNull();
        assertThat(credential.getUser().getId())
                .isEqualTo(user.getId());
        assertThat(credential.getFailedAttemptCount())
                .isZero();
    }

    @Test
    void shouldFindUserByNormalizedIdentifier() {
        User user = userRepository.saveAndFlush(
                createUser(
                        "Bob@example.com",
                        "Bob"));

        assertThat(
                userRepository.findByNormalizedEmail(
                        "bob@example.com"))
                .contains(user);

        assertThat(
                userRepository.findByNormalizedUsername(
                        "bob"))
                .contains(user);

        assertThat(
                userRepository
                        .findByNormalizedEmailOrNormalizedUsername(
                                "bob@example.com",
                                "not-used"))
                .contains(user);

        assertThat(
                userRepository
                        .findByNormalizedEmailOrNormalizedUsername(
                                "not-used@example.com",
                                "bob"))
                .contains(user);
    }

    @Test
    void shouldRejectDuplicateNormalizedEmail() {
        userRepository.saveAndFlush(
                createUser(
                        "First@example.com",
                        "first-user"));

        User duplicate = createUser(
                "FIRST@example.com",
                "second-user");

        assertThatThrownBy(
                () -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateNormalizedUsername() {
        userRepository.saveAndFlush(
                createUser(
                        "first@example.com",
                        "SameUser"));

        User duplicate = createUser(
                "second@example.com",
                "sameuser");

        assertThatThrownBy(
                () -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectSecondProfileForSameUser() {
        User user = userRepository.saveAndFlush(
                createUser(
                        "profile@example.com",
                        "profile-user"));

        userProfileRepository.saveAndFlush(
                new UserProfile(
                        user,
                        "First",
                        "Profile",
                        null));

        UserProfile duplicate = new UserProfile(
                user,
                "Second",
                "Profile",
                null);

        assertThatThrownBy(
                () -> userProfileRepository.saveAndFlush(
                        duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectSecondCredentialForSameUser() {
        User user = userRepository.saveAndFlush(
                createUser(
                        "credential@example.com",
                        "credential-user"));

        userCredentialRepository.saveAndFlush(
                new UserCredential(
                        user,
                        "{bcrypt}first-hash",
                        "bcrypt",
                        now()));

        UserCredential duplicate = new UserCredential(
                user,
                "{bcrypt}second-hash",
                "bcrypt",
                now());

        assertThatThrownBy(
                () -> userCredentialRepository.saveAndFlush(
                        duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistCredentialFailureState() {
        User user = userRepository.saveAndFlush(
                createUser(
                        "failure@example.com",
                        "failure-user"));

        UserCredential credential =
                userCredentialRepository.saveAndFlush(
                        new UserCredential(
                                user,
                                "{bcrypt}test-hash",
                                "bcrypt",
                                now()));

        OffsetDateTime failedAt = now();

        credential.recordFailedAttempt(failedAt);
        credential.recordFailedAttempt(failedAt);

        userCredentialRepository.saveAndFlush(credential);

        UserCredential persisted = userCredentialRepository
                .findByUser_Id(user.getId())
                .orElseThrow();

        assertThat(persisted.getFailedAttemptCount())
                .isEqualTo(2);

        assertThat(persisted.getLastFailedAt())
                .isEqualTo(failedAt);
    }

    private User createUser(
            String email,
            String username) {

        return new User(
                email,
                email.toLowerCase(Locale.ROOT),
                username,
                username.toLowerCase(Locale.ROOT));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
