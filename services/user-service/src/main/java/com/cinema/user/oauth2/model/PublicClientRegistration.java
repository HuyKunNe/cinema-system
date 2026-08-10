package com.cinema.user.oauth2.model;

import java.util.Set;

public record PublicClientRegistration(
        String clientId,
        String clientName,
        Set<String> redirectUris,
        Set<String> postLogoutRedirectUris,
        Set<String> scopes) {
}

