package com.cinema.common.security.context;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.common.security.constant.SecurityConstants;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static AuthenticationUser getCurrentUser() {
        Authentication authentication = currentAuthentication();

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticationUser authenticationUser) {
            return authenticationUser;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return fromJwt(jwtAuthentication.getToken());
        }

        if (principal instanceof Jwt jwt) {
            return fromJwt(jwt);
        }

        throw new AuthenticationCredentialsNotFoundException(
                "Unsupported authenticated principal");
    }

    public static Authentication getAuthentication() {
        return currentAuthentication();
    }

    private static Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication is required");
        }

        return authentication;
    }

    private static AuthenticationUser fromJwt(Jwt jwt) {
        UUID userId = parseUserId(jwt.getSubject());

        String username = jwt.getClaimAsString(
                SecurityConstants.CLAIM_USERNAME);

        Set<String> roles = readStringSet(
                jwt.getClaim(SecurityConstants.CLAIM_ROLES));

        Set<String> permissions = readStringSet(
                jwt.getClaim(SecurityConstants.CLAIM_PERMISSIONS));

        return new AuthenticationUser(
                userId,
                username,
                roles,
                permissions);
    }

    private static UUID parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException(
                    "JWT subject is missing");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException(
                    "JWT subject is not a valid UUID",
                    exception);
        }
    }

    private static Set<String> readStringSet(Object claimValue) {
        if (!(claimValue instanceof Collection<?> values)) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>();

        values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(result::add);

        return Set.copyOf(result);
    }

    private static boolean isAnonymous(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        return principal instanceof String value
                && "anonymousUser".equals(value);
    }
}
