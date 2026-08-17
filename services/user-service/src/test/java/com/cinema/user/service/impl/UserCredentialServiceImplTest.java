package com.cinema.user.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private UserCredentialServiceImpl userCredentialService;

    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-07T03:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final String RAW_PASSWORD = "correct-password-123";

    private static final String CURRENT_PASSWORD = "current-password-123";

    private static final String NEW_PASSWORD = "new-secure-password-456";

    private static final String CURRENT_HASH = "{bcrypt}$2a$10$current";

    private static final String NEW_HASH = "{bcrypt}$2a$10$new";

    private static final String USERNAME = "member";

    @BeforeEach
    void setUp() {
        userCredentialService = new UserCredentialServiceImpl(
                userRepository,
                userCredentialRepository,
                authorizationSessionRevocationService,
                passwordEncoder,
                FIXED_CLOCK);
    }

    @Test
    void createCredentialShouldEncodeAndPersistPassword() {
        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userCredentialRepository
                .existsByUser_Id(USER_ID))
                .thenReturn(false);

        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(NEW_HASH);

        userCredentialService.createCredential(
                USER_ID,
                RAW_PASSWORD);

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(
                UserCredential.class);

        verify(userCredentialRepository)
                .saveAndFlush(captor.capture());

        UserCredential credential = captor.getValue();

        assertThat(credential.getUser())
                .isSameAs(user);

        assertThat(credential.getPasswordHash())
                .isEqualTo(NEW_HASH);

        assertThat(credential.getPasswordHashAlgorithm())
                .isEqualTo("bcrypt");

        assertThat(credential.getPasswordChangedAt())
                .isEqualTo(FIXED_TIME);

        assertThat(credential.getFailedAttemptCount())
                .isZero();

        verify(passwordEncoder)
                .encode(RAW_PASSWORD);

        verify(userRepository)
                .findById(USER_ID);

        verify(userCredentialRepository)
                .existsByUser_Id(USER_ID);

        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void createCredentialShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCredentialService.createCredential(
                USER_ID,
                RAW_PASSWORD))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(
                userCredentialRepository,
                passwordEncoder);
    }

    @Test
    void createCredentialShouldRejectExistingCredential() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(createUser()));

        when(userCredentialRepository
                .existsByUser_Id(USER_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> userCredentialService.createCredential(
                USER_ID,
                RAW_PASSWORD))
                .isInstanceOf(ConflictException.class);

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userCredentialRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void createCredentialShouldTranslateConcurrentDuplicate() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(createUser()));

        when(userCredentialRepository
                .existsByUser_Id(USER_ID))
                .thenReturn(false);

        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(NEW_HASH);

        when(userCredentialRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate"));

        assertThatThrownBy(() -> userCredentialService.createCredential(
                USER_ID,
                RAW_PASSWORD))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createCredentialShouldRejectShortPassword() {
        assertThatThrownBy(() -> userCredentialService.createCredential(
                USER_ID,
                "short"))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
                userRepository,
                userCredentialRepository,
                passwordEncoder);
    }

    @Test
    void createCredentialShouldRejectPasswordExceedingBcryptByteLimit() {
        String oversizedPassword = "🔐".repeat(19);

        assertThat(oversizedPassword.length())
                .isGreaterThanOrEqualTo(12);

        assertThat(oversizedPassword
                .getBytes(StandardCharsets.UTF_8))
                .hasSizeGreaterThan(72);

        assertThatThrownBy(() -> userCredentialService.createCredential(
                USER_ID,
                oversizedPassword))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
                userRepository,
                userCredentialRepository,
                passwordEncoder);
    }

    @Test
    void changePasswordShouldReplaceEncodedPassword() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                CURRENT_PASSWORD,
                CURRENT_HASH))
                .thenReturn(true);

        when(passwordEncoder.matches(
                NEW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(false);

        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(NEW_HASH);

        userCredentialService.changePassword(
                USER_ID,
                CURRENT_PASSWORD,
                NEW_PASSWORD);

        assertThat(credential.getPasswordHash())
                .isEqualTo(NEW_HASH);

        assertThat(credential.getPasswordHashAlgorithm())
                .isEqualTo("bcrypt");

        assertThat(credential.getPasswordChangedAt())
                .isEqualTo(FIXED_TIME);

        verify(authorizationSessionRevocationService)
                .revokeByPrincipalName(
                        USERNAME,
                        RevocationReason.PASSWORD_CHANGED);
    }

    @Test
    void changePasswordShouldRejectIncorrectCurrentPassword() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                CURRENT_PASSWORD,
                CURRENT_HASH))
                .thenReturn(false);

        assertThatThrownBy(() -> userCredentialService.changePassword(
                USER_ID,
                CURRENT_PASSWORD,
                NEW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class);

        verify(passwordEncoder, never())
                .encode(anyString());

        assertThat(credential.getPasswordHash())
                .isEqualTo(CURRENT_HASH);

        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void changePasswordShouldRejectSamePassword() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                CURRENT_PASSWORD,
                CURRENT_HASH))
                .thenReturn(true);

        when(passwordEncoder.matches(
                NEW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(true);

        assertThatThrownBy(() -> userCredentialService.changePassword(
                USER_ID,
                CURRENT_PASSWORD,
                NEW_PASSWORD))
                .isInstanceOf(ValidationException.class);

        verify(passwordEncoder, never())
                .encode(anyString());

        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void changePasswordShouldThrowWhenCredentialDoesNotExist() {
        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCredentialService.changePassword(
                USER_ID,
                CURRENT_PASSWORD,
                NEW_PASSWORD))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(passwordEncoder);

        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void verifyPasswordShouldReturnFalseWhenCredentialDoesNotExist() {
        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.empty());

        boolean result = userCredentialService.verifyPassword(
                USER_ID,
                RAW_PASSWORD);

        assertThat(result).isFalse();
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void verifyPasswordShouldReturnFalseForIncorrectPassword() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(false);

        assertThat(userCredentialService.verifyPassword(
                USER_ID,
                RAW_PASSWORD))
                .isFalse();

        verify(passwordEncoder, never())
                .upgradeEncoding(anyString());
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void verifyPasswordShouldReturnTrueWithoutUpgrade() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(true);

        when(passwordEncoder.upgradeEncoding(
                CURRENT_HASH))
                .thenReturn(false);

        assertThat(userCredentialService.verifyPassword(
                USER_ID,
                RAW_PASSWORD))
                .isTrue();

        assertThat(credential.getPasswordHash())
                .isEqualTo(CURRENT_HASH);

        verify(passwordEncoder, never())
                .encode(anyString());
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void verifyPasswordShouldUpgradeEncoding() {
        UserCredential credential = createCredential(CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(true);

        when(passwordEncoder.upgradeEncoding(
                CURRENT_HASH))
                .thenReturn(true);

        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(NEW_HASH);

        assertThat(userCredentialService.verifyPassword(
                USER_ID,
                RAW_PASSWORD))
                .isTrue();

        assertThat(credential.getPasswordHash())
                .isEqualTo(NEW_HASH);

        assertThat(credential.getPasswordChangedAt())
                .isEqualTo(FIXED_TIME);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void verifyPasswordShouldRejectOversizedCandidateWithoutCallingEncoder() {
        String oversizedPassword = "a".repeat(73);

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(
                        createCredential(CURRENT_HASH)));

        assertThat(userCredentialService.verifyPassword(
                USER_ID,
                oversizedPassword))
                .isFalse();

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(
                authorizationSessionRevocationService);
    }

    @Test
    void resetPasswordShouldReplacePasswordAndRevokeAuthorizations() {
        UserCredential credential = createCredential(
                CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(
                        USER_ID))
                .thenReturn(
                        Optional.of(
                                credential));

        when(passwordEncoder.matches(
                NEW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(
                        false);

        when(passwordEncoder.encode(
                NEW_PASSWORD))
                .thenReturn(
                        NEW_HASH);

        userCredentialService.resetPassword(
                USER_ID,
                NEW_PASSWORD);

        assertThat(credential.getPasswordHash())
                .isEqualTo(
                        NEW_HASH);

        assertThat(credential.getPasswordHashAlgorithm())
                .isEqualTo(
                        "bcrypt");

        assertThat(credential.getPasswordChangedAt())
                .isEqualTo(
                        FIXED_TIME);

        verify(authorizationSessionRevocationService)
                .revokeByPrincipalName(
                        USERNAME,
                        RevocationReason.PASSWORD_RESET);
    }

    @Test
    void resetPasswordShouldRejectSamePasswordWithoutRevocation() {
        UserCredential credential = createCredential(
                CURRENT_HASH);

        when(userCredentialRepository
                .findByUser_Id(
                        USER_ID))
                .thenReturn(
                        Optional.of(
                                credential));

        when(passwordEncoder.matches(
                NEW_PASSWORD,
                CURRENT_HASH))
                .thenReturn(
                        true);

        assertThatThrownBy(() -> userCredentialService.resetPassword(
                USER_ID,
                NEW_PASSWORD))
                .isInstanceOf(
                        ValidationException.class);

        verify(passwordEncoder, never())
                .encode(
                        anyString());

        verifyNoInteractions(
                authorizationSessionRevocationService);

        assertThat(credential.getPasswordHash())
                .isEqualTo(
                        CURRENT_HASH);
    }

    @Test
    void resetPasswordShouldThrowWhenCredentialDoesNotExist() {
        when(userCredentialRepository
                .findByUser_Id(
                        USER_ID))
                .thenReturn(
                        Optional.empty());

        assertThatThrownBy(() -> userCredentialService.resetPassword(
                USER_ID,
                NEW_PASSWORD))
                .isInstanceOf(
                        NotFoundException.class);

        verifyNoInteractions(
                passwordEncoder,
                authorizationSessionRevocationService);
    }

    @Test
    void resetPasswordShouldRejectInvalidNewPassword() {
        assertThatThrownBy(() -> userCredentialService.resetPassword(
                USER_ID,
                "short"))
                .isInstanceOf(
                        ValidationException.class);

        verifyNoInteractions(
                userRepository,
                userCredentialRepository,
                passwordEncoder,
                authorizationSessionRevocationService);
    }

    private User createUser() {
        return new User(
                "member@example.com",
                "member@example.com",
                "member",
                "member");
    }

    private UserCredential createCredential(
            String passwordHash) {

        return new UserCredential(
                createUser(),
                passwordHash,
                "bcrypt",
                FIXED_TIME.minusDays(1));
    }
}
