package com.cinema.user.security.audit;

public record SecurityAuditContext(SecurityAuditActor actor, String correlationId) {}
