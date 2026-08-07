package com.cinema.user.entity;

import java.time.OffsetDateTime;

import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_normalized_email", columnNames = "normalized_email"),
        @UniqueConstraint(name = "uk_users_normalized_username", columnNames = "normalized_username")
})
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "normalized_username", nullable = false, length = 100)
    private String normalizedUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "disabled_at")
    private OffsetDateTime disabledAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    protected User() {
    }

    public User(
            String email,
            String normalizedEmail,
            String username,
            String normalizedUsername) {

        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.username = username;
        this.normalizedUsername = normalizedUsername;
        this.status = AccountStatus.PENDING_VERIFICATION;
    }

    public void verifyEmail(
            OffsetDateTime verifiedAt) {

        requireTimestamp(verifiedAt);

        if (status != AccountStatus.PENDING_VERIFICATION) {

            throw invalidTransition();
        }

        this.status = AccountStatus.ACTIVE;
        this.emailVerifiedAt = verifiedAt;
        this.lockedAt = null;
        this.disabledAt = null;
    }

    public void unlock() {
        if (status != AccountStatus.LOCKED) {
            throw invalidTransition();
        }

        this.status = AccountStatus.ACTIVE;
        this.lockedAt = null;
    }

    public void disable(
            OffsetDateTime disabledAt) {

        requireTimestamp(disabledAt);

        if (status == AccountStatus.DISABLED) {
            throw invalidTransition();
        }

        this.status = AccountStatus.DISABLED;
        this.disabledAt = disabledAt;
    }

    public void enable() {
        if (status != AccountStatus.DISABLED) {
            throw invalidTransition();
        }

        this.status = emailVerifiedAt == null
                ? AccountStatus.PENDING_VERIFICATION
                : AccountStatus.ACTIVE;

        this.disabledAt = null;
        this.lockedAt = null;
    }

    public void lock(
            OffsetDateTime lockedAt) {

        requireTimestamp(lockedAt);

        if (status != AccountStatus.ACTIVE) {
            throw invalidTransition();
        }

        this.status = AccountStatus.LOCKED;
        this.lockedAt = lockedAt;
    }

    public void recordSuccessfulLogin(
            OffsetDateTime loggedInAt) {

        requireTimestamp(loggedInAt);

        if (status != AccountStatus.ACTIVE) {
            throw invalidTransition();
        }

        this.lastLoginAt = loggedInAt;
    }

    private static void requireTimestamp(
            OffsetDateTime timestamp) {

        if (timestamp == null) {
            throw new ValidationException(
                    UserErrorCode.ACCOUNT_TIMESTAMP_REQUIRED);
        }
    }

    private static ConflictException invalidTransition() {

        return new ConflictException(
                UserErrorCode.ACCOUNT_STATE_TRANSITION_NOT_ALLOWED);
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getUsername() {
        return username;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public OffsetDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public OffsetDateTime getDisabledAt() {
        return disabledAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
