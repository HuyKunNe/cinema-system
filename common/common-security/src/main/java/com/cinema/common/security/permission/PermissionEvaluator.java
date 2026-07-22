package com.cinema.common.security.permission;

import com.cinema.common.security.authentication.CurrentUser;

public final class PermissionEvaluator {

    private PermissionEvaluator() {
    }

    public static boolean hasPermission(String permission) {

        return CurrentUser
                .get()
                .permissions()
                .contains(permission);

    }

}
