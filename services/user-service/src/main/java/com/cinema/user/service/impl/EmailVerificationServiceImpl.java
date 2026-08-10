package com.cinema.user.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
import com.cinema.user.service.EmailVerificationService;
import com.cinema.user.service.UserAccountLifecycleService;
import com.cinema.user.service.model.IssuedEmailVerificationToken;

@Service
@Validated
@Transactional(readOnly = true)
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Pattern RAW_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final UserRepository userRepository;

    private final EmailVerificationTokenRepository tokenRepository;

    private final UserAccountLifecycleService lifecycleService;

    private final EmailVerificationTokenCodec tokenCodec;

    private final EmailVerificationProperties properties;

    private final Clock clock;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            UserAccountLifecycleService lifecycleService,
            EmailVerificationTokenCodec tokenCodec,
            EmailVerificationProperties properties,
            Clock clock) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.lifecycleService = lifecycleService;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedEmailVerificationToken issue(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != AccountStatus.PENDING_VERIFICATION) {

            throw new ConflictException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_ISSUE_NOT_ALLOWED);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        List<EmailVerificationToken> activeTokens = tokenRepository
                .findAllActiveByUserIdForUpdate(userId);

        activeTokens.forEach(token -> token.revoke(now));

        String rawToken = tokenCodec.generateRawToken();

        String tokenHash = tokenCodec.hash(rawToken);

        OffsetDateTime expiresAt = now.plus(properties.lifetime());

        EmailVerificationToken token = new EmailVerificationToken(
                user,
                tokenHash,
                expiresAt);

        try {
            tokenRepository.saveAndFlush(token);
        } catch (DataIntegrityViolationException exception) {
            throw new InternalServerException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_ISSUE_FAILED,
                    exception);
        }

        return new IssuedEmailVerificationToken(
                rawToken,
                expiresAt);
    }

    @Override
    @Transactional
    public void confirm(String rawToken) {
        if (!isValidRawTokenFormat(rawToken)) {
            throw invalidToken();
        }

        String tokenHash = tokenCodec.hash(rawToken);

        EmailVerificationToken token = tokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(EmailVerificationServiceImpl::invalidToken);

        OffsetDateTime now = OffsetDateTime.now(clock);

        try {
            token.markUsed(now);

            lifecycleService.verifyEmail(token.getUser().getId());
        } catch (ConflictException exception) {
            throw new UnauthorizedException(UserErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID,
                    exception);
        }
    }

    private static boolean isValidRawTokenFormat(
            String rawToken) {

        return rawToken != null
                && RAW_TOKEN_PATTERN
                        .matcher(rawToken)
                        .matches();
    }

    private static UnauthorizedException invalidToken() {

        return new UnauthorizedException(UserErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
    }
}
