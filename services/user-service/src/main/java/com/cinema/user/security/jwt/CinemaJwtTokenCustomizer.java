package com.cinema.user.security.jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import com.cinema.user.config.JwtClaimsProperties;
import com.cinema.user.security.CinemaUserDetails;

@Component
public class CinemaJwtTokenCustomizer
        implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private static final String ROLE_PREFIX = "ROLE_";

    private static final String USERNAME_CLAIM = "username";
    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "permissions";

    private final JwtClaimsProperties properties;

    public CinemaJwtTokenCustomizer(JwtClaimsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }

        context.getClaims()
                .audience(properties.audiences());

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                context.getAuthorizationGrantType())) {

            customizeServiceToken(context);
            return;
        }

        customizeUserToken(context);
    }

    private void customizeUserToken(JwtEncodingContext context) {
        Authentication authentication = context.getPrincipal();
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CinemaUserDetails userDetails)) {
            throw new IllegalStateException(
                    "Authenticated user principal is invalid for JWT issuance");
        }

        UUID userId = userDetails.getUserId();

        if (userId.version() != 7) {
            throw new IllegalStateException(
                    "JWT user subject must use UUID version 7");
        }

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .distinct()
                .sorted()
                .toList();

        Set<String> authorizedScopes = context.getAuthorizedScopes();

        List<String> permissions = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith(ROLE_PREFIX))
                .filter(authorizedScopes::contains)
                .distinct()
                .sorted()
                .toList();

        context.getClaims()
                .subject(userId.toString())
                .claim(USERNAME_CLAIM, userDetails.getUsername())
                .claim(ROLES_CLAIM, roles)
                .claim(PERMISSIONS_CLAIM, permissions);
    }

    private void customizeServiceToken(JwtEncodingContext context) {
        String clientId = context.getRegisteredClient().getClientId();

        List<String> permissions = context.getAuthorizedScopes()
                .stream()
                .distinct()
                .sorted()
                .toList();

        context.getClaims()
                .subject(clientId)
                .claim(PERMISSIONS_CLAIM, permissions);
    }
}
