package com.cinema.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.enums.AccountStatus;

public class UserAccountLifecycleTest {
    private static final OffsetDateTime VERIFIED_AT = OffsetDateTime.parse(
            "2026-08-07T01:00:00Z");

    private static final OffsetDateTime LOCKED_AT = OffsetDateTime.parse(
            "2026-08-07T02:00:00Z");

    private static final OffsetDateTime DISABLED_AT = OffsetDateTime.parse(
            "2026-08-07T03:00:00Z");

    private static final OffsetDateTime LOGGED_IN_AT = OffsetDateTime.parse(
            "2026-08-07T04:00:00Z");

    private User createPendingUser() {
        return new User(
                "member@example.com",
                "member@example.com",
                "member",
                "member");
    }

    private User createActiveUser() {
        User user = createPendingUser();
        user.verifyEmail(VERIFIED_AT);
        return user;
    }

    private User createLockedUser() {
        User user = createActiveUser();
        user.lock(LOCKED_AT);
        return user;
    }

    @Test
    void newUserShouldRequireEmailVerification() {
        User user = createPendingUser();

        assertThat(user.getStatus())
                .isEqualTo(
                        AccountStatus.PENDING_VERIFICATION);

        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getLockedAt()).isNull();
        assertThat(user.getDisabledAt()).isNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    void verifyEmailShouldActivatePendingUser() {
        User user = createPendingUser();

        user.verifyEmail(VERIFIED_AT);

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(user.getEmailVerifiedAt())
                .isEqualTo(VERIFIED_AT);

        assertThat(user.getLockedAt()).isNull();
        assertThat(user.getDisabledAt()).isNull();
    }

    @Test
    void verifyEmailShouldRejectRepeatedVerification() {
        User user = createActiveUser();

        assertThatThrownBy(() -> user.verifyEmail(
                VERIFIED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(user.getEmailVerifiedAt())
                .isEqualTo(VERIFIED_AT);
    }

    @Test
    void verifyEmailShouldRequireTimestamp() {
        User user = createPendingUser();

        assertThatThrownBy(() -> user.verifyEmail(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThat(user.getStatus())
                .isEqualTo(
                        AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void lockShouldLockActiveUser() {
        User user = createActiveUser();

        user.lock(LOCKED_AT);

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.LOCKED);

        assertThat(user.getLockedAt())
                .isEqualTo(LOCKED_AT);
    }

    @Test
    void lockShouldRejectPendingUser() {
        User user = createPendingUser();

        assertThatThrownBy(() -> user.lock(LOCKED_AT))
                .isInstanceOf(ConflictException.class);

        assertThat(user.getLockedAt()).isNull();
    }

    @Test
    void lockShouldRequireTimestamp() {
        User user = createActiveUser();

        assertThatThrownBy(() -> user.lock(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void unlockShouldActivateLockedUser() {
        User user = createLockedUser();

        user.unlock();

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(user.getLockedAt()).isNull();
    }

    @Test
    void unlockShouldRejectUserThatIsNotLocked() {
        User user = createActiveUser();

        assertThatThrownBy(user::unlock)
                .isInstanceOf(ConflictException.class);

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void disableShouldSupportPendingActiveAndLockedUsers() {
        User pending = createPendingUser();
        User active = createActiveUser();
        User locked = createLockedUser();

        pending.disable(DISABLED_AT);
        active.disable(DISABLED_AT);
        locked.disable(DISABLED_AT);

        assertThat(List.of(
                pending,
                active,
                locked))
                .allSatisfy(user -> {
                    assertThat(user.getStatus())
                            .isEqualTo(
                                    AccountStatus.DISABLED);

                    assertThat(user.getDisabledAt())
                            .isEqualTo(DISABLED_AT);
                });
    }

    @Test
    void disableShouldRejectAlreadyDisabledUser() {
        User user = createActiveUser();
        user.disable(DISABLED_AT);

        assertThatThrownBy(() -> user.disable(
                DISABLED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(user.getDisabledAt())
                .isEqualTo(DISABLED_AT);
    }

    @Test
    void disableShouldRequireTimestamp() {
        User user = createActiveUser();

        assertThatThrownBy(() -> user.disable(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void enableShouldReturnUnverifiedUserToPending() {
        User user = createPendingUser();
        user.disable(DISABLED_AT);

        user.enable();

        assertThat(user.getStatus())
                .isEqualTo(
                        AccountStatus.PENDING_VERIFICATION);

        assertThat(user.getDisabledAt()).isNull();
        assertThat(user.getLockedAt()).isNull();
    }

    @Test
    void enableShouldReactivateVerifiedUser() {
        User user = createActiveUser();
        user.disable(DISABLED_AT);

        user.enable();

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(user.getEmailVerifiedAt())
                .isEqualTo(VERIFIED_AT);

        assertThat(user.getDisabledAt()).isNull();
    }

    @Test
    void enableShouldClearPreviousLockState() {
        User user = createLockedUser();
        user.disable(DISABLED_AT);

        user.enable();

        assertThat(user.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(user.getLockedAt()).isNull();
        assertThat(user.getDisabledAt()).isNull();
    }

    @Test
    void enableShouldRejectUserThatIsNotDisabled() {
        User user = createActiveUser();

        assertThatThrownBy(user::enable)
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordSuccessfulLoginShouldUpdateActiveUser() {
        User user = createActiveUser();

        user.recordSuccessfulLogin(LOGGED_IN_AT);

        assertThat(user.getLastLoginAt())
                .isEqualTo(LOGGED_IN_AT);
    }

    @Test
    void recordSuccessfulLoginShouldRejectInactiveUser() {
        User pending = createPendingUser();
        User locked = createLockedUser();

        User disabled = createActiveUser();
        disabled.disable(DISABLED_AT);

        assertThatThrownBy(() -> pending.recordSuccessfulLogin(
                LOGGED_IN_AT))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> locked.recordSuccessfulLogin(
                LOGGED_IN_AT))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> disabled.recordSuccessfulLogin(
                LOGGED_IN_AT))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordSuccessfulLoginShouldRequireTimestamp() {
        User user = createActiveUser();

        assertThatThrownBy(() -> user.recordSuccessfulLogin(null))
                .isInstanceOf(
                        ValidationException.class);

        assertThat(user.getLastLoginAt()).isNull();
    }
}
