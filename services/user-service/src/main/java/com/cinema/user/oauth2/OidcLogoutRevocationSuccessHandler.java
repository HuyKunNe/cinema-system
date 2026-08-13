package com.cinema.user.oauth2;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.web.authentication.OidcLogoutAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OidcLogoutRevocationSuccessHandler
        implements AuthenticationSuccessHandler {

    private static final OAuth2TokenType ID_TOKEN_TYPE = new OAuth2TokenType(
            "id_token");

    private final OAuth2AuthorizationService authorizationService;

    private final AuthenticationSuccessHandler delegate = new OidcLogoutAuthenticationSuccessHandler();

    public OidcLogoutRevocationSuccessHandler(
            OAuth2AuthorizationService authorizationService) {

        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (authentication instanceof OidcLogoutAuthenticationToken logoutAuthentication) {

            revokeAuthorization(
                    logoutAuthentication);
        }

        delegate.onAuthenticationSuccess(
                request,
                response,
                authentication);
    }

    private void revokeAuthorization(
            OidcLogoutAuthenticationToken logoutAuthentication) {

        OAuth2Authorization authorization = authorizationService.findByToken(
                logoutAuthentication
                        .getIdToken()
                        .getTokenValue(),
                ID_TOKEN_TYPE);

        if (authorization == null) {
            return;
        }

        OAuth2Authorization.Builder revokedAuthorization = OAuth2Authorization.from(
                authorization);

        if (authorization.getAccessToken() != null
                && !authorization
                        .getAccessToken()
                        .isInvalidated()) {

            revokedAuthorization.invalidate(
                    authorization
                            .getAccessToken()
                            .getToken());
        }

        if (authorization.getRefreshToken() != null
                && !authorization
                        .getRefreshToken()
                        .isInvalidated()) {

            revokedAuthorization.invalidate(
                    authorization
                            .getRefreshToken()
                            .getToken());
        }

        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(
                OidcIdToken.class);

        if (idToken != null
                && !idToken.isInvalidated()) {

            revokedAuthorization.invalidate(
                    idToken.getToken());
        }

        authorizationService.save(
                revokedAuthorization.build());
    }
}
