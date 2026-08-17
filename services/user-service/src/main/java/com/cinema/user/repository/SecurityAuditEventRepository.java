package com.cinema.user.repository;

import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, UUID> {

    List<SecurityAuditEvent> findAllByEventTypeOrderByOccurredAtDesc(
            SecurityAuditEventType eventType);

    List<SecurityAuditEvent> findAllByActorTypeAndActorReferenceOrderByOccurredAtDesc(
            SecurityAuditActorType actorType, String actorReference);

    List<SecurityAuditEvent> findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
            SecurityAuditTargetType targetType, String targetReference);
}
