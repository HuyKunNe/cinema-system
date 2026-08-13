package com.cinema.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.RevocationAuditEvent;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;

public interface RevocationAuditEventRepository
        extends JpaRepository<RevocationAuditEvent, UUID> {

    List<RevocationAuditEvent> findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
            RevocationAuditTargetType targetType,
            String targetReference);
}
