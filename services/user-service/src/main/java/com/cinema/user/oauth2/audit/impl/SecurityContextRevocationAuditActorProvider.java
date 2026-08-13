package com.cinema.user.oauth2.audit.impl;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.common.security.constant.SecurityConstants;
import com.cinema.user.oauth2.audit.RevocationAuditActor;
import com.cinema.user.oauth2.audit.RevocationAuditActorProvider;
import com.cinema.user.security.CinemaUserDetails;

@Component
public class SecurityContextRevocationAuditActorProvider
        implements RevocationAuditActorProvider {

    @Override
    public RevocationAuditActor currentActor() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return systemActor();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CinemaUserDetails userDetails) {
            return new RevocationAuditActor(
                    userDetails.getUserId(),
                    userDetails.getUsername());
        }

        if (principal instanceof AuthenticationUser user) {
            return new RevocationAuditActor(
                    user.userId(),
                    actorName(
                            user.username(),
                            authentication.getName()));
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

            return fromJwt(
                    jwtAuthentication.getToken(),
                    authentication.getName());
        }

        if (principal instanceof Jwt jwt) {
            return fromJwt(
                    jwt,
                    authentication.getName());
        }

        if (principal instanceof UserDetails userDetails) {
            return new RevocationAuditActor(
                    null,
                    actorName(
                            userDetails.getUsername(),
                            authentication.getName()));
        }

        return new RevocationAuditActor(
                null,
                actorName(
                        authentication.getName(),
                        CommonConstants.SYSTEM));
    }

    private static RevocationAuditActor fromJwt(
            Jwt jwt,
            String authenticationName) {

        String username = jwt.getClaimAsString(
                SecurityConstants.CLAIM_USERNAME);

        return new RevocationAuditActor(
                parseUuid(
                        jwt.getSubject()),
                actorName(
                        username,
                        authenticationName));
    }

    private static UUID parseUuid(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {
            return UUID.fromString(
                    value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String actorName(
            String preferred,
            String fallback) {

        if (preferred != null
                && !preferred.isBlank()) {

            return preferred.trim();
        }

        if (fallback != null
                && !fallback.isBlank()) {

            return fallback.trim();
        }

        return CommonConstants.SYSTEM;
    }

    private static RevocationAuditActor systemActor() {
        return new RevocationAuditActor(
                null,
                CommonConstants.SYSTEM);
    }
}
