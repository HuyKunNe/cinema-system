package com.cinema.user.oauth2.model;

import java.util.Set;

public record ConfidentialUserClientRegistration(
        String clientId,
        String clientName,
        String rawClientSecret,
        Set<String> redirectUris,
        Set<String> postLogoutRedirectUris,
        Set<String> scopes) {

    @Override
    public String toString() {
        return "ConfidentialUserClientRegistration"
                + "[clientId="
                + clientId
                + ", clientName="
                + clientName
                + ", rawClientSecret=[REDACTED]"
                + ", redirectUris="
                + redirectUris
                + ", postLogoutRedirectUris="
                + postLogoutRedirectUris
                + ", scopes="
                + scopes
                + "]";
    }
}
