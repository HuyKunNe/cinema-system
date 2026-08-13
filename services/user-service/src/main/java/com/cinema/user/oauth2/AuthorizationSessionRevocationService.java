package com.cinema.user.oauth2;

public interface AuthorizationSessionRevocationService {

    void revokeByPrincipalName(
            String principalName);

    void revokeByRegisteredClientId(
            String registeredClientId);
}
