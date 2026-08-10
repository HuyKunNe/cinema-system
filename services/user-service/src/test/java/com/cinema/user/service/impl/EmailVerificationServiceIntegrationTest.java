package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.EmailVerificationToken;
import com.cinema.user.entity.User;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.repository.EmailVerificationTokenRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.EmailVerificationTokenCodec;
import com.cinema.user.service.EmailVerificationService;
import com.cinema.user.service.model.IssuedEmailVerificationToken;

@SpringBootTest(properties = {
        "spring.main.web-application-type=none",
        "cinema.user.email-verification.token-lifetime=PT24H"
})
class EmailVerificationServiceIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EmailVerificationTokenCodec tokenCodec;

    @Autowired
    private Clock clock;

    @Test
    void shouldIssueVerificationTokenForPendingUser() {
        User user = createPendingUser("issue");
        OffsetDateTime issuedAfter = OffsetDateTime.now(clock);

        IssuedEmailVerificationToken issued = emailVerificationService.issue(user.getId());

        String tokenHash = tokenCodec.hash(issued.getRawToken());
        EmailVerificationToken persisted = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow();

        assertThat(issued.getRawToken())
                .hasSize(43)
                .matches("^[A-Za-z0-9_-]{43}$");

        assertThat(persisted.getTokenHash())
                .isEqualTo(tokenHash)
                .isNotEqualTo(issued.getRawToken());

        assertThat(persisted.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(persisted.getUsedAt()).isNull();
        assertThat(persisted.getRevokedAt()).isNull();

        assertThat(persisted.getExpiresAt())
                .isAfter(issuedAfter)
                .isEqualTo(issued.getExpiresAt());
    }

    @Test
    void shouldRevokePreviousActiveTokenWhenIssuingNewToken() {
        User user = createPendingUser("reissue");

        IssuedEmailVerificationToken first = emailVerificationService.issue(user.getId());

        IssuedEmailVerificationToken second = emailVerificationService.issue(user.getId());

        EmailVerificationToken firstPersisted = tokenRepository
                .findByTokenHash(tokenCodec.hash(first.getRawToken()))
                .orElseThrow();

        EmailVerificationToken secondPersisted = tokenRepository
                .findByTokenHash(tokenCodec.hash(second.getRawToken()))
                .orElseThrow();

        assertThat(second.getRawToken())
                .isNotEqualTo(first.getRawToken());

        assertThat(firstPersisted.getRevokedAt()).isNotNull();
        assertThat(firstPersisted.getUsedAt()).isNull();

        assertThat(secondPersisted.getRevokedAt()).isNull();
        assertThat(secondPersisted.getUsedAt()).isNull();

        assertThat(activeTokens(user.getId()))
                .extracting(EmailVerificationToken::getId)
                .containsExactly(secondPersisted.getId());
    }

    @Test
    void shouldConfirmTokenAndActivatePendingUser() {
        User user = createPendingUser("confirm");
        IssuedEmailVerificationToken issued = emailVerificationService.issue(user.getId());

        emailVerificationService.confirm(issued.getRawToken());

        User verifiedUser = userRepository.findById(user.getId())
                .orElseThrow();

        EmailVerificationToken usedToken = tokenRepository
                .findByTokenHash(tokenCodec.hash(issued.getRawToken()))
                .orElseThrow();

        assertThat(verifiedUser.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
        assertThat(verifiedUser.getEmailVerifiedAt()).isNotNull();

        assertThat(usedToken.getUsedAt()).isNotNull();
        assertThat(usedToken.getRevokedAt()).isNull();
        assertThat(activeTokens(user.getId())).isEmpty();
    }

    @Test
    void shouldRejectReusingConfirmedToken() {
        User user = createPendingUser("reuse");
        IssuedEmailVerificationToken issued = emailVerificationService.issue(user.getId());

        emailVerificationService.confirm(issued.getRawToken());

        assertThatThrownBy(() -> emailVerificationService.confirm(issued.getRawToken()))
                .isInstanceOf(UnauthorizedException.class);

        EmailVerificationToken persisted = tokenRepository
                .findByTokenHash(tokenCodec.hash(issued.getRawToken()))
                .orElseThrow();

        assertThat(persisted.getUsedAt()).isNotNull();
        assertThat(persisted.getRevokedAt()).isNull();
    }

    @Test
    void shouldRejectRevokedTokenWithoutActivatingUser() {
        User user = createPendingUser("revoked");

        IssuedEmailVerificationToken revoked = emailVerificationService.issue(user.getId());

        IssuedEmailVerificationToken active = emailVerificationService.issue(user.getId());

        assertThatThrownBy(() -> emailVerificationService.confirm(revoked.getRawToken()))
                .isInstanceOf(UnauthorizedException.class);

        User unchangedUser = userRepository.findById(user.getId())
                .orElseThrow();

        EmailVerificationToken activeToken = tokenRepository
                .findByTokenHash(tokenCodec.hash(active.getRawToken()))
                .orElseThrow();

        assertThat(unchangedUser.getStatus())
                .isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(unchangedUser.getEmailVerifiedAt()).isNull();

        assertThat(activeToken.getUsedAt()).isNull();
        assertThat(activeToken.getRevokedAt()).isNull();

        assertThat(activeTokens(user.getId()))
                .extracting(EmailVerificationToken::getId)
                .containsExactly(activeToken.getId());
    }

    @Test
    void shouldRejectUnknownTokenWithoutChangingUser() {
        User user = createPendingUser("unknown");
        String unknownRawToken = "A".repeat(43);

        assertThatThrownBy(() -> emailVerificationService.confirm(unknownRawToken))
                .isInstanceOf(UnauthorizedException.class);

        User unchangedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getStatus())
                .isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(unchangedUser.getEmailVerifiedAt()).isNull();
    }

    private User createPendingUser(String prefix) {
        String suffix = UUID.randomUUID().toString();
        String email = prefix + "." + suffix + "@example.com";
        String username = prefix + "." + suffix;

        User user = userRepository.saveAndFlush(new User(
                email,
                email,
                username,
                username));

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.PENDING_VERIFICATION);

        return user;
    }

    private List<EmailVerificationToken> activeTokens(UUID userId) {
        return tokenRepository
                .findAllByUser_IdAndUsedAtIsNullAndRevokedAtIsNull(userId);
    }
}
