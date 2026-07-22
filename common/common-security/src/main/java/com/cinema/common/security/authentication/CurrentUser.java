package com.cinema.common.security.authentication;

import com.cinema.common.security.context.SecurityContextUtils;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {

        return SecurityContextUtils
                .getCurrentUser()
                .userId();
    }

    public static String username() {

        return SecurityContextUtils
                .getCurrentUser()
                .username();
    }

    public static AuthenticationUser get() {

        return SecurityContextUtils
                .getCurrentUser();
    }

}
