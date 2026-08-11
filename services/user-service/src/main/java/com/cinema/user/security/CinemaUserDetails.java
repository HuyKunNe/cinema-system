package com.cinema.user.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cinema.user.enums.AccountStatus;

public final class CinemaUserDetails implements UserDetails, CredentialsContainer {
    private final UUID userId;
    private final String username;
    private String password;
    private final AccountStatus status;
    private final List<GrantedAuthority> authorities;

    public CinemaUserDetails(
            UUID userId,
            String username,
            String password,
            AccountStatus status,
            Collection<? extends GrantedAuthority> authorities) {

        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        // The password is legitimately null after CredentialsContainer erases it.
        // OAuth2 authorization persistence may reconstruct the principal after
        // that point, so null must remain a valid deserialization value.
        this.password = password;
        this.status = Objects.requireNonNull(status);

        Objects.requireNonNull(authorities);

        TreeMap<String, GrantedAuthority> uniqueAuthorities = new TreeMap<>();

        for (GrantedAuthority authority : authorities) {
            if (authority == null
                    || authority.getAuthority() == null
                    || authority.getAuthority().isBlank()) {

                continue;
            }

            uniqueAuthorities.putIfAbsent(
                    authority.getAuthority(),
                    authority);
        }

        this.authorities = Collections.unmodifiableList(
                new ArrayList<>(uniqueAuthorities.values()));
    }

    public UUID getUserId() {
        return userId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status.isAuthenticationAllowed();
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
