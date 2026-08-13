package com.cinema.user.oauth2;

import java.util.List;

public interface OAuth2AuthorizationQueryRepository {

    List<String> findIdsByPrincipalName(
            String principalName);

    List<String> findIdsByRegisteredClientId(
            String registeredClientId);
}
