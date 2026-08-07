package com.cinema.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.EmailVerificationToken;
import com.cinema.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Transactional
class EmailVerificationTokenRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private OffsetDateTime now;
    private OffsetDateTime expiresAt;

    @BeforeEach
    void setUpTime() {
        now = OffsetDateTime.now(clock);
        expiresAt = now.plusHours(1);
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString();

        return userRepository.saveAndFlush(new User(
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix,
                prefix + "." + suffix));
    }

    private EmailVerificationToken createToken(
            User user,
            char hashCharacter) {

        return new EmailVerificationToken(
                user,
                String.valueOf(hashCharacter)
                        .repeat(64),
                expiresAt);
    }

    @Test
    void shouldPersistAndFindTokenByHash() {
        User user = createUser("persist");

        EmailVerificationToken token = tokenRepository.saveAndFlush(
                createToken(user, 'a'));

        entityManager.clear();

        EmailVerificationToken found = tokenRepository.findByTokenHash(
                "a".repeat(64))
                .orElseThrow();

        assertThat(found.getId())
                .isEqualTo(token.getId());

        assertThat(found.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(found.getExpiresAt())
                .isEqualTo(expiresAt);
    }

    @Test
    void shouldLoadTokenForUpdate() {
        User user = createUser("lock");

        EmailVerificationToken token = tokenRepository.saveAndFlush(
                createToken(user, 'b'));

        entityManager.clear();

        EmailVerificationToken locked = tokenRepository
                .findByTokenHashForUpdate(
                        "b".repeat(64))
                .orElseThrow();

        assertThat(locked.getId())
                .isEqualTo(token.getId());
    }

    @Test
    void shouldFindOnlyUnusedAndUnrevokedTokensForUser() {
        User user = createUser("active");

        EmailVerificationToken active = createToken(user, 'c');

        EmailVerificationToken used = createToken(user, 'd');

        EmailVerificationToken revoked = createToken(user, 'e');

        used.markUsed(now);
        revoked.revoke(now);

        tokenRepository.saveAllAndFlush(
                List.of(
                        active,
                        used,
                        revoked));

        entityManager.clear();

        assertThat(tokenRepository
                .findAllByUser_IdAndUsedAtIsNullAndRevokedAtIsNull(
                        user.getId()))
                .extracting(
                        EmailVerificationToken::getTokenHash)
                .containsExactly("c".repeat(64));
    }

    @Test
    void shouldRejectDuplicateTokenHash() {
        User firstUser = createUser("first");
        User secondUser = createUser("second");

        tokenRepository.saveAndFlush(
                createToken(firstUser, 'f'));

        entityManager.clear();

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(
                createToken(secondUser, 'f')))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void shouldDetectExistingTokenHash() {
        User user = createUser("exists");

        tokenRepository.saveAndFlush(
                createToken(user, '1'));

        assertThat(tokenRepository.existsByTokenHash(
                "1".repeat(64)))
                .isTrue();

        assertThat(tokenRepository.existsByTokenHash(
                "2".repeat(64)))
                .isFalse();
    }
}
