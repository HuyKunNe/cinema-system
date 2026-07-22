package com.cinema.common.security.context;

import com.cinema.common.security.authentication.AuthenticationUser;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static AuthenticationUser getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        return (AuthenticationUser) authentication.getPrincipal();

    }

}
