package com.cinema.user.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "security_audit_events",
        indexes = {
            @Index(name = "idx_security_audit_event_time", columnList = "event_type, occurred_at"),
            @Index(
                    name = "idx_security_audit_actor",
                    columnList = "actor_type, actor_reference, occurred_at"),
            @Index(
                    name = "idx_security_audit_target",
                    columnList = "target_type, target_reference, occurred_at"),
            @Index(name = "idx_security_audit_outcome", columnList = "outcome, occurred_at"),
            @Index(name = "idx_security_audit_correlation", columnList = "correlation_id")
        })
public class SecurityAuditEvent extends BaseEntity {

    private static final int ACTOR_REFERENCE_MAX_LENGTH = 200;

    private static final int TARGET_REFERENCE_MAX_LENGTH = 200;

    private static final int CORRELATION_ID_MAX_LENGTH = 100;

    private static final int REASON_MAX_LENGTH = 100;

    private static final int METADATA_MAX_LENGTH = 1000;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 60)
    private SecurityAuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, updatable = false, length = 20)
    private SecurityAuditActorType actorType;

    @Column(name = "actor_reference", nullable = false, updatable = false, length = 200)
    private String actorReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", updatable = false, length = 30)
    private SecurityAuditTargetType targetType;

    @Column(name = "target_reference", updatable = false, length = 200)
    private String targetReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private SecurityAuditOutcome outcome;

    @Column(name = "correlation_id", updatable = false, length = 100)
    private String correlationId;

    @Column(name = "reason", updatable = false, length = 100)
    private String reason;

    @Column(name = "metadata", updatable = false, length = 1000)
    private String metadata;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    protected SecurityAuditEvent() {}

    public SecurityAuditEvent(
            SecurityAuditEventType eventType,
            SecurityAuditActorType actorType,
            String actorReference,
            SecurityAuditTargetType targetType,
            String targetReference,
            SecurityAuditOutcome outcome,
            String correlationId,
            String reason,
            String metadata,
            OffsetDateTime occurredAt) {

        if (eventType == null || actorType == null || outcome == null || occurredAt == null) {

            throw invalidAuditEvent();
        }

        this.eventType = eventType;
        this.actorType = actorType;
        this.actorReference = requireText(actorReference, ACTOR_REFERENCE_MAX_LENGTH);

        validateTarget(targetType, targetReference);

        this.targetType = targetType;
        this.targetReference = optionalText(targetReference, TARGET_REFERENCE_MAX_LENGTH);

        this.outcome = outcome;
        this.correlationId = optionalText(correlationId, CORRELATION_ID_MAX_LENGTH);
        this.reason = optionalText(reason, REASON_MAX_LENGTH);
        this.metadata = optionalText(metadata, METADATA_MAX_LENGTH);
        this.occurredAt = occurredAt;
    }

    public SecurityAuditEventType getEventType() {
        return eventType;
    }

    public SecurityAuditActorType getActorType() {
        return actorType;
    }

    public String getActorReference() {
        return actorReference;
    }

    public SecurityAuditTargetType getTargetType() {
        return targetType;
    }

    public String getTargetReference() {
        return targetReference;
    }

    public SecurityAuditOutcome getOutcome() {
        return outcome;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReason() {
        return reason;
    }

    public String getMetadata() {
        return metadata;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    private static void validateTarget(SecurityAuditTargetType targetType, String targetReference) {

        boolean targetTypePresent = targetType != null;

        boolean targetReferencePresent = targetReference != null && !targetReference.isBlank();

        if (targetTypePresent != targetReferencePresent) {
            throw invalidAuditEvent();
        }
    }

    private static String requireText(String value, int maxLength) {

        String normalized = optionalText(value, maxLength);

        if (normalized == null) {
            throw invalidAuditEvent();
        }

        return normalized;
    }

    private static String optionalText(String value, int maxLength) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty() || normalized.length() > maxLength) {

            throw invalidAuditEvent();
        }

        return normalized;
    }

    private static ValidationException invalidAuditEvent() {
        return new ValidationException(UserErrorCode.SECURITY_AUDIT_EVENT_INVALID);
    }
}
