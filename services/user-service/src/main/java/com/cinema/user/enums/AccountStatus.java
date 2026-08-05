package com.cinema.user.enums;

public enum AccountStatus {

    PENDING_VERIFICATION(false),
    ACTIVE(true),
    LOCKED(false),
    DISABLED(false);

    private final boolean authenticationAllowed;

    AccountStatus(boolean authenticationAllowed) {
        this.authenticationAllowed = authenticationAllowed;
    }

    public boolean isAuthenticationAllowed() {
        return authenticationAllowed;
    }
}
