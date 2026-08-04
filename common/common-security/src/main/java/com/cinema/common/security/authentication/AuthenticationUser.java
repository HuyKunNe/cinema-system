package com.cinema.common.security.authentication;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticationUser(
        UUID userId,
        String username,
        Set<String> roles,
        Set<String> permissions) {

    public AuthenticationUser {
        Objects.requireNonNull(userId, "userId must not be null");

        roles = roles == null
                ? Set.of()
                : Set.copyOf(roles);

        permissions = permissions == null
                ? Set.of()
                : Set.copyOf(permissions);
    }
}
