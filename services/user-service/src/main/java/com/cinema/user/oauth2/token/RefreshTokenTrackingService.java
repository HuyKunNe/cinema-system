package com.cinema.user.oauth2.token;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

public interface RefreshTokenTrackingService {

    void synchronize(
            OAuth2Authorization previous,
            OAuth2Authorization current);
}
