package com.cinema.user.oauth2.impl;

import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2AuthorizationQueryRepository;
import com.cinema.user.oauth2.audit.RevocationAuditRecorder;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthorizationSessionRevocationServiceImpl
        implements AuthorizationSessionRevocationService {

    private final OAuth2AuthorizationQueryRepository queryRepository;

    private final OAuth2AuthorizationService authorizationService;

    private final RevocationAuditRecorder auditRecorder;

    public AuthorizationSessionRevocationServiceImpl(
            OAuth2AuthorizationQueryRepository queryRepository,
            OAuth2AuthorizationService authorizationService,
            RevocationAuditRecorder auditRecorder) {

        this.queryRepository = queryRepository;

        this.authorizationService = authorizationService;

        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public void revokeByPrincipalName(String principalName, RevocationReason reason) {

        int revokedAuthorizationCount =
                revokeAll(queryRepository.findIdsByPrincipalName(principalName));

        auditRecorder.record(
                RevocationAuditTargetType.USER, principalName, reason, revokedAuthorizationCount);
    }

    @Override
    @Transactional
    public void revokeByRegisteredClientId(
            String registeredClientId, String clientId, RevocationReason reason) {

        int revokedAuthorizationCount =
                revokeAll(queryRepository.findIdsByRegisteredClientId(registeredClientId));

        auditRecorder.record(
                RevocationAuditTargetType.CLIENT, clientId, reason, revokedAuthorizationCount);
    }

    private int revokeAll(List<String> authorizationIds) {

        int revokedAuthorizationCount = 0;

        for (String authorizationId : authorizationIds) {
            OAuth2Authorization authorization = authorizationService.findById(authorizationId);

            if (authorization == null) {
                continue;
            }

            if (revoke(authorization)) {

                revokedAuthorizationCount++;
            }
        }

        return revokedAuthorizationCount;
    }

    private boolean revoke(OAuth2Authorization authorization) {

        OAuth2Authorization.Builder builder = OAuth2Authorization.from(authorization);

        boolean accessTokenInvalidated = invalidateAccessToken(authorization, builder);

        boolean refreshTokenInvalidated = invalidateRefreshToken(authorization, builder);

        boolean idTokenInvalidated = invalidateIdToken(authorization, builder);

        boolean changed = accessTokenInvalidated || refreshTokenInvalidated || idTokenInvalidated;

        if (!changed) {
            return false;
        }

        authorizationService.save(builder.build());

        return true;
    }

    private static boolean invalidateAccessToken(
            OAuth2Authorization authorization, OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();

        if (accessToken == null || accessToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(accessToken.getToken());

        return true;
    }

    private static boolean invalidateRefreshToken(
            OAuth2Authorization authorization, OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getRefreshToken();

        if (refreshToken == null || refreshToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(refreshToken.getToken());

        return true;
    }

    private static boolean invalidateIdToken(
            OAuth2Authorization authorization, OAuth2Authorization.Builder builder) {

        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);

        if (idToken == null || idToken.isInvalidated()) {

            return false;
        }

        builder.invalidate(idToken.getToken());

        return true;
    }
}
