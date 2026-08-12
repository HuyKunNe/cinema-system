package com.cinema.user.oauth2.token;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

@Service
@Transactional(readOnly = true)
public class RefreshTokenTrackingServiceImpl
        implements RefreshTokenTrackingService {

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
    public void synchronize(
            OAuth2Authorization previous,
            OAuth2Authorization current) {

        Objects.requireNonNull(current);

        OAuth2RefreshToken currentRefreshToken = refreshToken(current);

        if (currentRefreshToken == null) {
            return;
        }

        OAuth2RefreshToken previousRefreshToken = refreshToken(previous);

        if (previousRefreshToken == null) {
            createHistory(
                    current,
                    currentRefreshToken);

            return;
        }

        if (previousRefreshToken
                .getTokenValue()
                .equals(currentRefreshToken
                        .getTokenValue())) {

            return;
        }

        OffsetDateTime rotatedAt = OffsetDateTime.now(clock);

        String previousTokenHash = refreshTokenHasher.hash(
                previousRefreshToken
                        .getTokenValue());

        RefreshTokenHistory previousHistory = refreshTokenHistoryRepository
                .findByTokenHashForUpdate(
                        previousTokenHash)
                .orElseThrow(
                        RefreshTokenTrackingServiceImpl::historyNotFound);

        previousHistory.markRotated(
                rotatedAt);

        refreshTokenHistoryRepository.save(
                previousHistory);

        createHistory(
                current,
                currentRefreshToken);
    }

    private void createHistory(
            OAuth2Authorization authorization,
            OAuth2RefreshToken refreshToken) {

        OffsetDateTime issuedAt = toOffsetDateTime(
                refreshToken.getIssuedAt());

        OffsetDateTime expiresAt = toOffsetDateTime(
                refreshToken.getExpiresAt());

        RefreshTokenHistory history = new RefreshTokenHistory(
                authorization.getId(),
                authorization
                        .getRegisteredClientId(),
                authorization
                        .getPrincipalName(),
                refreshTokenHasher.hash(
                        refreshToken
                                .getTokenValue()),
                issuedAt,
                expiresAt);

        refreshTokenHistoryRepository.save(
                history);
    }

    private static OAuth2RefreshToken refreshToken(
            OAuth2Authorization authorization) {

        if (authorization == null
                || authorization.getRefreshToken() == null) {

            return null;
        }

        return authorization
                .getRefreshToken()
                .getToken();
    }

    private static OffsetDateTime toOffsetDateTime(
            java.time.Instant instant) {

        if (instant == null) {
            throw new InternalServerException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_HISTORY_INVALID);
        }

        return OffsetDateTime.ofInstant(
                instant,
                ZoneOffset.UTC);
    }

    private static InternalServerException historyNotFound() {
        return new InternalServerException(
                UserErrorCode.OAUTH2_REFRESH_TOKEN_HISTORY_NOT_FOUND);
    }
}
