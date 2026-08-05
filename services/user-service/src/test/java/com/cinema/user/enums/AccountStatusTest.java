package com.cinema.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountStatusTest {

    @Test
    void activeAccountShouldAllowAuthentication() {
        assertThat(AccountStatus.ACTIVE.isAuthenticationAllowed())
                .isTrue();
    }

    @Test
    void inactiveAccountStatesShouldRejectAuthentication() {
        assertThat(AccountStatus.PENDING_VERIFICATION.isAuthenticationAllowed())
                .isFalse();

        assertThat(AccountStatus.LOCKED.isAuthenticationAllowed())
                .isFalse();

        assertThat(AccountStatus.DISABLED.isAuthenticationAllowed())
                .isFalse();
    }
}
