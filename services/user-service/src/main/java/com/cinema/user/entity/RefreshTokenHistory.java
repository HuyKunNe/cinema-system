package com.cinema.user.entity;

import java.time.OffsetDateTime;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.token.RefreshTokenStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "oauth2_refresh_token_history", indexes = {
        @Index(name = "idx_oauth2_refresh_token_history_authorization", columnList = "authorization_id"),
        @Index(name = "idx_oauth2_refresh_token_history_client_principal", columnList = "registered_client_id, principal_name"),
        @Index(name = "idx_oauth2_refresh_token_history_status_expires", columnList = "status, expires_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_oauth2_refresh_token_history_hash", columnNames = "token_hash")
})
public class RefreshTokenHistory extends BaseEntity {

    @Column(name = "authorization_id", nullable = false, length = 100, updatable = false)
    private String authorizationId;

    @Column(name = "registered_client_id", nullable = false, length = 100, updatable = false)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 200, updatable = false)
    private String principalName;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false, columnDefinition = "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefreshTokenStatus status = RefreshTokenStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "rotated_at")
    private OffsetDateTime rotatedAt;

    @Column(name = "reused_at")
    private OffsetDateTime reusedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected RefreshTokenHistory() {
    }

    public RefreshTokenHistory(
            String authorizationId,
            String registeredClientId,
            String principalName,
            String tokenHash,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {

        this.authorizationId = requireText(authorizationId);

        this.registeredClientId = requireText(registeredClientId);

        this.principalName = requireText(principalName);

        validateTokenHash(tokenHash);

        if (issuedAt == null
                || expiresAt == null
                || !expiresAt.isAfter(issuedAt)) {

            throw new ValidationException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_EXPIRATION_INVALID);
        }

        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = RefreshTokenStatus.ACTIVE;
    }

    public void markRotated(
            OffsetDateTime rotatedAt) {

        requireTimestamp(rotatedAt);

        if (status != RefreshTokenStatus.ACTIVE) {
            throw invalidTransition();
        }

        if (rotatedAt.isBefore(issuedAt)) {
            throw invalidTransition();
        }

        status = RefreshTokenStatus.ROTATED;
        this.rotatedAt = rotatedAt;
    }

    public void markRevoked(
            OffsetDateTime revokedAt) {

        requireTimestamp(revokedAt);

        if (status != RefreshTokenStatus.ACTIVE
                && status != RefreshTokenStatus.ROTATED) {

            throw invalidTransition();
        }

        if (revokedAt.isBefore(issuedAt)) {
            throw invalidTransition();
        }
        status = RefreshTokenStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    public void markReused(
            OffsetDateTime reusedAt) {

        requireTimestamp(reusedAt);

        if (status != RefreshTokenStatus.ROTATED) {
            throw invalidTransition();
        }

        if (rotatedAt != null
                && reusedAt.isBefore(rotatedAt)) {

            throw invalidTransition();
        }

        status = RefreshTokenStatus.REUSED;
        this.reusedAt = reusedAt;
    }

    public boolean isActive() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    public boolean isRotated() {
        return status == RefreshTokenStatus.ROTATED;
    }

    public boolean isRevoked() {
        return status == RefreshTokenStatus.REVOKED;
    }

    public boolean isReused() {
        return status == RefreshTokenStatus.REUSED;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public String getRegisteredClientId() {
        return registeredClientId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public RefreshTokenStatus getStatus() {
        return status;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRotatedAt() {
        return rotatedAt;
    }

    public OffsetDateTime getReusedAt() {
        return reusedAt;
    }

    private static String requireText(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new ValidationException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_HISTORY_INVALID);
        }

        return value.trim();
    }

    private static void validateTokenHash(
            String tokenHash) {

        if (tokenHash == null
                || !tokenHash.matches(
                        "^[0-9a-f]{64}$")) {

            throw new ValidationException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_HASH_INVALID);
        }
    }

    private static void requireTimestamp(
            OffsetDateTime timestamp) {

        if (timestamp == null) {
            throw new ValidationException(
                    UserErrorCode.OAUTH2_REFRESH_TOKEN_TIMESTAMP_REQUIRED);
        }
    }

    private static ConflictException invalidTransition() {
        return new ConflictException(
                UserErrorCode.OAUTH2_REFRESH_TOKEN_TRANSITION_NOT_ALLOWED);
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
