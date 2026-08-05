package com.cinema.user.entity;

import java.time.OffsetDateTime;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_credentials", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_credentials_user", columnNames = "user_id")
})
public class UserCredential extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_credentials_user"))
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_hash_algorithm", nullable = false, length = 50)
    private String passwordHashAlgorithm;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt;

    @Column(name = "failed_attempt_count", nullable = false)
    private int failedAttemptCount;

    @Column(name = "last_failed_at")
    private OffsetDateTime lastFailedAt;

    protected UserCredential() {
    }

    public UserCredential(
            User user,
            String passwordHash,
            String passwordHashAlgorithm,
            OffsetDateTime passwordChangedAt) {

        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordHashAlgorithm = passwordHashAlgorithm;
        this.passwordChangedAt = passwordChangedAt;
        this.failedAttemptCount = 0;
    }

    public void changePassword(
            String passwordHash,
            String passwordHashAlgorithm,
            OffsetDateTime changedAt) {

        this.passwordHash = passwordHash;
        this.passwordHashAlgorithm = passwordHashAlgorithm;
        this.passwordChangedAt = changedAt;
        this.failedAttemptCount = 0;
        this.lastFailedAt = null;
    }

    public void recordFailedAttempt(OffsetDateTime failedAt) {
        this.failedAttemptCount++;
        this.lastFailedAt = failedAt;
    }

    public void clearFailedAttempts() {
        this.failedAttemptCount = 0;
        this.lastFailedAt = null;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordHashAlgorithm() {
        return passwordHashAlgorithm;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public int getFailedAttemptCount() {
        return failedAttemptCount;
    }

    public OffsetDateTime getLastFailedAt() {
        return lastFailedAt;
    }
}
