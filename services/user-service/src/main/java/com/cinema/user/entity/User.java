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
