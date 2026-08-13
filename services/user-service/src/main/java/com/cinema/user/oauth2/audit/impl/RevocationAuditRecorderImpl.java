package com.cinema.user.oauth2.audit.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.user.entity.RevocationAuditEvent;
import com.cinema.user.oauth2.audit.RevocationAuditActor;
import com.cinema.user.oauth2.audit.RevocationAuditActorProvider;
import com.cinema.user.oauth2.audit.RevocationAuditRecorder;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.RevocationAuditEventRepository;

@Service
@Transactional(readOnly = true)
public class RevocationAuditRecorderImpl
        implements RevocationAuditRecorder {

    private final RevocationAuditEventRepository auditEventRepository;

    private final RevocationAuditActorProvider actorProvider;

    private final Clock clock;

    public RevocationAuditRecorderImpl(
            RevocationAuditEventRepository auditEventRepository,
            RevocationAuditActorProvider actorProvider,
            Clock clock) {

        this.auditEventRepository = auditEventRepository;

        this.actorProvider = actorProvider;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(
            RevocationAuditTargetType targetType,
            String targetReference,
            RevocationReason reason,
            int revokedAuthorizationCount) {

        RevocationAuditActor actor = actorProvider.currentActor();

        RevocationAuditEvent event = new RevocationAuditEvent(
                targetType,
                targetReference,
                reason,
                actor.userId(),
                actor.name(),
                revokedAuthorizationCount,
                OffsetDateTime.ofInstant(
                        clock.instant(),
                        ZoneOffset.UTC));

        auditEventRepository.save(
                event);
    }
}
