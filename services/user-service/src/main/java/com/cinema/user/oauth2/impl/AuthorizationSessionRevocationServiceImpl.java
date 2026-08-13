package com.cinema.user.oauth2.impl;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2AuthorizationQueryRepository;

@Service
@Transactional(readOnly = true)
public class AuthorizationSessionRevocationServiceImpl
        implements AuthorizationSessionRevocationService {

    private final OAuth2AuthorizationQueryRepository queryRepository;

    private final OAuth2AuthorizationService authorizationService;

    public AuthorizationSessionRevocationServiceImpl(
            OAuth2AuthorizationQueryRepository queryRepository,
            OAuth2AuthorizationService authorizationService) {

        this.queryRepository = queryRepository;

        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public void revokeByPrincipalName(
            String principalName) {

        revokeAll(
                queryRepository.findIdsByPrincipalName(
                        principalName));
    }

    @Override
    @Transactional
    public void revokeByRegisteredClientId(
            String registeredClientId) {

        revokeAll(
                queryRepository.findIdsByRegisteredClientId(
                        registeredClientId));
    }

    private void revokeAll(
            List<String> authorizationIds) {

        for (String authorizationId : authorizationIds) {
            OAuth2Authorization authorization = authorizationService.findById(
                    authorizationId);

            if (authorization == null) {
                continue;
            }

            revoke(
                    authorization);
        }
    }

    private void revoke(
            OAuth2Authorization authorization) {

        OAuth2Authorization.Builder builder = OAuth2Authorization.from(
                authorization);

        boolean changed = invalidateAccessToken(
                authorization,
                builder);

        changed = invalidateRefreshToken(
                authorization,
                builder)
                || changed;

        changed = invalidateIdToken(
                authorization,
                builder)
                || changed;

        if (changed) {
            authorizationService.save(
                    builder.build());
        }
    }

    private static boolean invalidateAccessToken(
            OAuth2Authorization authorization,
            OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();

        if (accessToken == null
                || accessToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(
                accessToken.getToken());

        return true;
    }

    private static boolean invalidateRefreshToken(
            OAuth2Authorization authorization,
            OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();

        if (refreshToken == null
                || refreshToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(
                refreshToken.getToken());

        return true;
    }

    private static boolean invalidateIdToken(
            OAuth2Authorization authorization,
            OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(
                OidcIdToken.class);

        if (idToken == null
                || idToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(
                idToken.getToken());

        return true;
    }
}
