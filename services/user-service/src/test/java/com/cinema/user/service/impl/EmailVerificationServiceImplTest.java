package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.user.config.EmailVerificationProperties;
import com.cinema.user.entity.EmailVerificationToken;
import com.cinema.user.entity.User;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.EmailVerificationTokenRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.EmailVerificationTokenCodec;
import com.cinema.user.service.UserAccountLifecycleService;
import com.cinema.user.service.model.IssuedEmailVerificationToken;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceImplTest {
    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final String RAW_TOKEN = "A".repeat(43);

    private static final String TOKEN_HASH = "a".repeat(64);

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-07T03:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final Duration LIFETIME = Duration.ofHours(24);

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserAccountLifecycleService lifecycleService;

    @Mock
    private EmailVerificationTokenCodec tokenCodec;

    private EmailVerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new EmailVerificationServiceImpl(
                userRepository,
                tokenRepository,
                lifecycleService,
                tokenCodec,
                new EmailVerificationProperties(
                        LIFETIME),
                FIXED_CLOCK);
    }

    @Test
    void issueShouldRevokeActiveTokensAndPersistHash() {
        User user = mock(User.class);
        EmailVerificationToken oldToken = mock(EmailVerificationToken.class);

        when(user.getStatus())
                .thenReturn(
                        AccountStatus.PENDING_VERIFICATION);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(tokenRepository
                .findAllActiveByUserIdForUpdate(
                        USER_ID))
                .thenReturn(List.of(oldToken));

        when(tokenCodec.generateRawToken())
                .thenReturn(RAW_TOKEN);

        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        IssuedEmailVerificationToken result = verificationService.issue(USER_ID);

        verify(oldToken).revoke(FIXED_TIME);

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(
                EmailVerificationToken.class);

        verify(tokenRepository)
                .saveAndFlush(captor.capture());

        EmailVerificationToken saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);

        assertThat(saved.getTokenHash())
                .isEqualTo(TOKEN_HASH);

        assertThat(saved.getExpiresAt())
                .isEqualTo(FIXED_TIME.plus(LIFETIME));

        assertThat(result.getRawToken())
                .isEqualTo(RAW_TOKEN);

        assertThat(result.getExpiresAt())
                .isEqualTo(FIXED_TIME.plus(LIFETIME));
    }

    @Test
    void issueShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.issue(USER_ID))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(
                tokenRepository,
                tokenCodec,
                lifecycleService);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {
            "ACTIVE",
            "LOCKED",
            "DISABLED"
    })
    void issueShouldRejectNonPendingAccount(
            AccountStatus status) {

        User user = mock(User.class);

        when(user.getStatus()).thenReturn(status);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> verificationService.issue(USER_ID))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(
                tokenRepository,
                tokenCodec,
                lifecycleService);
    }

    @Test
    void issueShouldTranslatePersistenceFailure() {
        User user = mock(User.class);

        when(user.getStatus())
                .thenReturn(AccountStatus.PENDING_VERIFICATION);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(tokenRepository
                .findAllActiveByUserIdForUpdate(USER_ID))
                .thenReturn(List.of());

        when(tokenCodec.generateRawToken())
                .thenReturn(RAW_TOKEN);

        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException("duplicate hash"));

        assertThatThrownBy(() -> verificationService.issue(USER_ID))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void confirmShouldConsumeTokenAndVerifyUser() {
        User user = mock(User.class);
        EmailVerificationToken token = mock(EmailVerificationToken.class);

        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository
                .findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        when(token.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);

        verificationService.confirm(RAW_TOKEN);

        InOrder order = inOrder(
                token,
                lifecycleService);

        order.verify(token).markUsed(FIXED_TIME);

        order.verify(lifecycleService)
                .verifyEmail(USER_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "short",
            "contains+invalid/characters",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    })
    void confirmShouldRejectInvalidRawTokenFormat(
            String rawToken) {

        assertThatThrownBy(() -> verificationService.confirm(rawToken))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(
                tokenCodec,
                tokenRepository,
                lifecycleService);
    }

    @Test
    void confirmShouldRejectUnknownToken() {
        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository
                .findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.confirm(
                RAW_TOKEN))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(lifecycleService);
    }

    @Test
    void confirmShouldHideUnusableTokenState() {
        EmailVerificationToken token = mock(EmailVerificationToken.class);

        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository
                .findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        doThrow(new ConflictException(
                UserErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED))
                .when(token)
                .markUsed(FIXED_TIME);

        assertThatThrownBy(() -> verificationService.confirm(
                RAW_TOKEN))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(lifecycleService);
    }

    @Test
    void confirmShouldHideAccountTransitionFailure() {
        User user = mock(User.class);
        EmailVerificationToken token = mock(EmailVerificationToken.class);

        when(tokenCodec.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository
                .findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        when(token.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);

        doThrow(new ConflictException(
                UserErrorCode.ACCOUNT_STATE_TRANSITION_NOT_ALLOWED))
                .when(lifecycleService)
                .verifyEmail(USER_ID);

        assertThatThrownBy(() -> verificationService.confirm(
                RAW_TOKEN))
                .isInstanceOf(UnauthorizedException.class);

        verify(token).markUsed(FIXED_TIME);
    }
}
