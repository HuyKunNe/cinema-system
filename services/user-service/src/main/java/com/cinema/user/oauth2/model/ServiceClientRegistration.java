package com.cinema.user.oauth2.model;

import java.util.Set;

public record ServiceClientRegistration(
        String clientId,
        String clientName,
        String rawClientSecret,
        Set<String> scopes) {

    @Override
    public String toString() {
        return "ServiceClientRegistration"
                + "[clientId="
                + clientId
                + ", clientName="
                + clientName
                + ", rawClientSecret=[REDACTED]"
                + ", scopes="
                + scopes
                + "]";
    }
}
