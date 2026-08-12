package com.cinema.user.oauth2.token;

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

public interface RefreshTokenReuseService {

    boolean detectAndRevoke(
            String rawRefreshToken,
            OAuth2AuthorizationService authorizationService);
}
