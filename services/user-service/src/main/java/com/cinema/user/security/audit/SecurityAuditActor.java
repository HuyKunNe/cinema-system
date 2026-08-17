package com.cinema.user.security.audit;

public record SecurityAuditActor(SecurityAuditActorType type, String reference) {}
