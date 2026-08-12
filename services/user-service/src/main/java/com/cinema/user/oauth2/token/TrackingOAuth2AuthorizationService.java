package com.cinema.user.oauth2.token;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.transaction.annotation.Transactional;

public class TrackingOAuth2AuthorizationService
        implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationService delegate;
    private final RefreshTokenTrackingService trackingService;

    public TrackingOAuth2AuthorizationService(
            OAuth2AuthorizationService delegate,
            RefreshTokenTrackingService trackingService) {

        this.delegate = delegate;
        this.trackingService = trackingService;
    }

    @Override
    @Transactional
    public void save(
            OAuth2Authorization authorization) {

        OAuth2Authorization previous = delegate.findById(
                authorization.getId());

        delegate.save(authorization);

        trackingService.synchronize(
                previous,
                authorization);
    }

    @Override
    @Transactional
    public void remove(
            OAuth2Authorization authorization) {

        delegate.remove(authorization);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findById(
            String id) {

        return delegate.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findByToken(
            String token,
            OAuth2TokenType tokenType) {

        return delegate.findByToken(
                token,
                tokenType);
    }
}
