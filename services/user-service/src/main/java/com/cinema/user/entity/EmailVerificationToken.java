package com.cinema.user.entity;

import java.time.OffsetDateTime;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.exception.UserErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verification_tokens_user_active", columnList = "user_id, used_at, revoked_at, expires_at"),
        @Index(name = "idx_email_verification_tokens_expires_at", columnList = "expires_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_email_verification_tokens_hash", columnNames = "token_hash")
})
public class EmailVerificationToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_email_verification_tokens_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(
            User user,
            String tokenHash,
            OffsetDateTime expiresAt) {

        if (user == null) {
            throw new ValidationException(
                    UserErrorCode.USER_NOT_FOUND);
        }

        validateTokenHash(tokenHash);

        if (expiresAt == null) {
            throw new ValidationException(
                    UserErrorCode.EMAIL_VERIFICATION_EXPIRATION_REQUIRED);
        }

        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(OffsetDateTime now) {
        requireTimestamp(now);

        return usedAt == null
                && revokedAt == null
                && now.isBefore(expiresAt);
    }

    public void markUsed(OffsetDateTime usedAt) {
        requireTimestamp(usedAt);

        if (this.usedAt != null
                || revokedAt != null) {

            throw new ConflictException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_USABLE);
        }

        if (!usedAt.isBefore(expiresAt)) {
            throw new ConflictException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
        }

        this.usedAt = usedAt;
    }

    public void revoke(OffsetDateTime revokedAt) {
        requireTimestamp(revokedAt);

        if (usedAt != null
                || this.revokedAt != null) {

            throw new ConflictException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_USABLE);
        }

        this.revokedAt = revokedAt;
    }

    private static void validateTokenHash(
            String tokenHash) {

        if (tokenHash == null
                || !tokenHash.matches(
                        "^[0-9a-f]{64}$")) {

            throw new ValidationException(
                    UserErrorCode.EMAIL_VERIFICATION_TOKEN_HASH_INVALID);
        }
    }

    private static void requireTimestamp(
            OffsetDateTime timestamp) {

        if (timestamp == null) {
            throw new ValidationException(
                    UserErrorCode.EMAIL_VERIFICATION_TIMESTAMP_REQUIRED);
        }
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
