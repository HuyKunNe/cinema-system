package com.cinema.user.oauth2;

public interface OAuth2ClientLifecycleService {

    void deactivate(
            String clientId);

    void rotateSecret(
            String clientId,
            String newRawClientSecret);
}
