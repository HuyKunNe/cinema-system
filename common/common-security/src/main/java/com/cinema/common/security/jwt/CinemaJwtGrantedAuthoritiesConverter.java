package com.cinema.common.security.jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.cinema.common.security.constant.SecurityConstants;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CinemaJwtGrantedAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(
                authorities,
                jwt.getClaim(SecurityConstants.CLAIM_ROLES));

        addPermissions(
                authorities,
                jwt.getClaim(SecurityConstants.CLAIM_PERMISSIONS));

        return Set.copyOf(authorities);
    }

    private void addRoles(
            Set<GrantedAuthority> authorities,
            Object claimValue) {

        readClaimValues(claimValue).stream()
                .map(this::normalizeRole)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private void addPermissions(
            Set<GrantedAuthority> authorities,
            Object claimValue) {

        readClaimValues(claimValue).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private Set<String> readClaimValues(Object claimValue) {
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

    private String normalizeRole(String role) {
        if (role.startsWith(SecurityConstants.ROLE_PREFIX)) {
            return role;
        }

        return SecurityConstants.ROLE_PREFIX + role;
    }
}
