package com.cinema.user.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "oauth2_revocation_audit_events", indexes = {
        @Index(name = "idx_oauth2_revocation_audit_target", columnList = "target_type, target_reference, occurred_at"),
        @Index(name = "idx_oauth2_revocation_audit_actor", columnList = "actor_user_id, occurred_at"),
        @Index(name = "idx_oauth2_revocation_audit_reason", columnList = "reason, occurred_at")
})
public class RevocationAuditEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 20)
    private RevocationAuditTargetType targetType;

    @Column(name = "target_reference", nullable = false, updatable = false, length = 200)
    private String targetReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 50)
    private RevocationReason reason;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false, updatable = false, length = 200)
    private String actorName;

    @Column(name = "revoked_authorization_count", nullable = false, updatable = false)
    private int revokedAuthorizationCount;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    protected RevocationAuditEvent() {
    }

    public RevocationAuditEvent(
            RevocationAuditTargetType targetType,
            String targetReference,
            RevocationReason reason,
            UUID actorUserId,
            String actorName,
            int revokedAuthorizationCount,
            OffsetDateTime occurredAt) {

        if (targetType == null
                || reason == null
                || revokedAuthorizationCount < 0
                || occurredAt == null) {

            throw invalidAuditEvent();
        }

        this.targetType = targetType;

        this.targetReference = requireText(
                targetReference);

        this.reason = reason;

        this.actorUserId = actorUserId;

        this.actorName = requireText(
                actorName);

        this.revokedAuthorizationCount = revokedAuthorizationCount;

        this.occurredAt = occurredAt;
    }

    public RevocationAuditTargetType getTargetType() {
        return targetType;
    }

    public String getTargetReference() {
        return targetReference;
    }

    public RevocationReason getReason() {
        return reason;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public int getRevokedAuthorizationCount() {
        return revokedAuthorizationCount;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    private static String requireText(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw invalidAuditEvent();
        }

        return value.trim();
    }

    private static ValidationException invalidAuditEvent() {
        return new ValidationException(
                UserErrorCode.OAUTH2_REVOCATION_AUDIT_INVALID);
    }
}
