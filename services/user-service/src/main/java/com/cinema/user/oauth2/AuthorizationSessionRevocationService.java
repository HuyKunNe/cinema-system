package com.cinema.user.oauth2;

import com.cinema.user.oauth2.audit.RevocationReason;

public interface AuthorizationSessionRevocationService {

    void revokeByPrincipalName(
            String principalName,
            RevocationReason reason);

    void revokeByRegisteredClientId(
            String registeredClientId,
            String clientId,
            RevocationReason reason);
}
