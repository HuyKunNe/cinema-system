package com.cinema.user.oauth2;

import java.util.UUID;

public interface AdministrativeAuthorizationService {

    void revokeUserAuthorizations(
            UUID userId);

    void revokeClientAuthorizations(
            String clientId);

    void deactivateClient(
            String clientId);

    void rotateClientSecret(
            String clientId,
            String newRawClientSecret);
}
