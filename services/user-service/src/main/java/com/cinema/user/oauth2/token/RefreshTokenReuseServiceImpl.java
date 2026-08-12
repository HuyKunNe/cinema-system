package com.cinema.user.oauth2.token;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

@Service
@Transactional(readOnly = true)
public class RefreshTokenReuseServiceImpl
        implements RefreshTokenReuseService {

    private final RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    private final RefreshTokenHasher refreshTokenHasher;

    private final Clock clock;

    public RefreshTokenReuseServiceImpl(
            RefreshTokenHistoryRepository refreshTokenHistoryRepository,
            RefreshTokenHasher refreshTokenHasher,
            Clock clock) {

        this.refreshTokenHistoryRepository = refreshTokenHistoryRepository;

        this.refreshTokenHasher = refreshTokenHasher;

        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean detectAndRevoke(
            String rawRefreshToken,
            OAuth2AuthorizationService authorizationService) {

        Objects.requireNonNull(
                authorizationService);

        String tokenHash = refreshTokenHasher.hash(
                rawRefreshToken);

        RefreshTokenHistory reusedHistory = refreshTokenHistoryRepository
                .findByTokenHashForUpdate(
                        tokenHash)
                .orElse(null);

        if (reusedHistory == null
                || !reusedHistory.isRotated()) {

            return false;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        String authorizationId = reusedHistory
                .getAuthorizationId();

        /*
         * Flush REUSED before executing the bulk revoke query.
         * The bulk query clears the persistence context.
         */
        reusedHistory.markReused(now);

        refreshTokenHistoryRepository
                .saveAndFlush(
                        reusedHistory);

        refreshTokenHistoryRepository
                .revokeActiveTokensByAuthorizationId(
                        authorizationId,
                        RefreshTokenStatus.ACTIVE,
                        RefreshTokenStatus.REVOKED,
                        now);

        OAuth2Authorization authorization = authorizationService.findById(
                authorizationId);

        if (authorization != null) {
            OAuth2Authorization invalidated = invalidateAuthorization(
                    authorization);

            authorizationService.save(
                    invalidated);
        }

        return true;
    }

    private static OAuth2Authorization invalidateAuthorization(
            OAuth2Authorization authorization) {

        OAuth2Authorization.Builder builder = OAuth2Authorization.from(
                authorization);

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization
                .getAccessToken();

        if (accessToken != null) {
            builder.token(
                    accessToken.getToken(),
                    metadata -> {
                        metadata.putAll(
                                accessToken
                                        .getMetadata());

                        metadata.put(
                                OAuth2Authorization.Token.INVALIDATED_METADATA_NAME,
                                true);
                    });
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization
                .getRefreshToken();

        if (refreshToken != null) {
            builder.token(
                    refreshToken.getToken(),
                    metadata -> {
                        metadata.putAll(
                                refreshToken
                                        .getMetadata());

                        metadata.put(
                                OAuth2Authorization.Token.INVALIDATED_METADATA_NAME,
                                true);
                    });
        }

        return builder.build();
    }
}
