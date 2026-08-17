package com.cinema.user.security.audit;

public record SecurityAuditRecord(
        SecurityAuditEventType eventType,
        SecurityAuditTargetType targetType,
        String targetReference,
        SecurityAuditOutcome outcome,
        String reason,
        String metadata) {}
