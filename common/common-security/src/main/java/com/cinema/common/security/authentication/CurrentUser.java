package com.cinema.common.security.authentication;

import java.util.Set;
import java.util.UUID;

import com.cinema.common.security.context.SecurityContextUtils;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id() {
        return get().userId();
    }

    public static String username() {
        return get().username();
    }

    public static Set<String> roles() {
        return get().roles();
    }

    public static Set<String> permissions() {
        return get().permissions();
    }

    public static AuthenticationUser get() {
        return SecurityContextUtils.getCurrentUser();
    }
}
