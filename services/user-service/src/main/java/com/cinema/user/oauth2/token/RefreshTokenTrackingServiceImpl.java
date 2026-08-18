package com.cinema.user.oauth2.token;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RefreshTokenTrackingServiceImpl implements RefreshTokenTrackingService {

    private final RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    private final RefreshTokenHasher refreshTokenHasher;

    private final Clock clock;

    public RefreshTokenTrackingServiceImpl(
            RefreshTokenHistoryRepository refreshTokenHistoryRepository,
            RefreshTokenHasher refreshTokenHasher,
            Clock clock) {

        this.refreshTokenHistoryRepository = refreshTokenHistoryRepository;

        this.refreshTokenHasher = refreshTokenHasher;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void synchronize(OAuth2Authorization previous, OAuth2Authorization current) {

        Objects.requireNonNull(current);

        OAuth2RefreshToken currentRefreshToken = refreshToken(current);

        if (currentRefreshToken == null) {
            return;
        }

        OAuth2RefreshToken previousRefreshToken = refreshToken(previous);

        if (previousRefreshToken == null) {
            createHistory(current, currentRefreshToken);

            return;
        }

        if (previousRefreshToken.getTokenValue().equals(currentRefreshToken.getTokenValue())) {

            synchronizeRevocation(previous, current, currentRefreshToken);

            return;
        }

        OffsetDateTime rotatedAt = OffsetDateTime.now(clock);

        String previousTokenHash = refreshTokenHasher.hash(previousRefreshToken.getTokenValue());

        RefreshTokenHistory previousHistory =
                refreshTokenHistoryRepository
                        .findByTokenHashForUpdate(previousTokenHash)
                        .orElseThrow(RefreshTokenTrackingServiceImpl::historyNotFound);

        if (!previousHistory.isActive()) {
            throw invalidGrant();
        }

        previousHistory.markRotated(rotatedAt);

        refreshTokenHistoryRepository.save(previousHistory);

        createHistory(current, currentRefreshToken);
    }

    private void synchronizeRevocation(
            OAuth2Authorization previous,
            OAuth2Authorization current,
            OAuth2RefreshToken currentRefreshToken) {

        if (previous.getRefreshToken() == null || current.getRefreshToken() == null) {

            return;
        }

        boolean previouslyInvalidated = previous.getRefreshToken().isInvalidated();

        boolean currentlyInvalidated = current.getRefreshToken().isInvalidated();

        if (!previouslyInvalidated && currentlyInvalidated) {
            revokeHistory(currentRefreshToken);
        }
    }

    private void revokeHistory(OAuth2RefreshToken refreshToken) {

        OffsetDateTime revokedAt = OffsetDateTime.now(clock);

        String tokenHash = refreshTokenHasher.hash(refreshToken.getTokenValue());

        RefreshTokenHistory history =
                refreshTokenHistoryRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(RefreshTokenTrackingServiceImpl::historyNotFound);

        history.markRevoked(revokedAt);

        refreshTokenHistoryRepository.save(history);
    }

    private void createHistory(OAuth2Authorization authorization, OAuth2RefreshToken refreshToken) {

        OffsetDateTime issuedAt = toOffsetDateTime(refreshToken.getIssuedAt());

        OffsetDateTime expiresAt = toOffsetDateTime(refreshToken.getExpiresAt());

        RefreshTokenHistory history =
                new RefreshTokenHistory(
                        authorization.getId(),
                        authorization.getRegisteredClientId(),
                        authorization.getPrincipalName(),
                        refreshTokenHasher.hash(refreshToken.getTokenValue()),
                        issuedAt,
                        expiresAt);

        refreshTokenHistoryRepository.save(history);
    }

    private static OAuth2RefreshToken refreshToken(OAuth2Authorization authorization) {

        if (authorization == null || authorization.getRefreshToken() == null) {

            return null;
        }

        return authorization.getRefreshToken().getToken();
    }

    private static OffsetDateTime toOffsetDateTime(java.time.Instant instant) {

        if (instant == null) {
            throw new InternalServerException(UserErrorCode.OAUTH2_REFRESH_TOKEN_HISTORY_INVALID);
        }

        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static InternalServerException historyNotFound() {
        return new InternalServerException(UserErrorCode.OAUTH2_REFRESH_TOKEN_HISTORY_NOT_FOUND);
    }

    private static OAuth2AuthenticationException invalidGrant() {
        return new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
    }
}
