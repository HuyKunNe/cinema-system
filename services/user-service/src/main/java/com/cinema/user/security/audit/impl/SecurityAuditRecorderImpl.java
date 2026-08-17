package com.cinema.user.security.audit.impl;

import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.security.audit.SecurityAuditActor;
import com.cinema.user.security.audit.SecurityAuditContext;
import com.cinema.user.security.audit.SecurityAuditContextProvider;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class SecurityAuditRecorderImpl implements SecurityAuditRecorder {

    private final SecurityAuditEventRepository auditEventRepository;

    private final SecurityAuditContextProvider contextProvider;

    private final Clock clock;

    public SecurityAuditRecorderImpl(
            SecurityAuditEventRepository auditEventRepository,
            SecurityAuditContextProvider contextProvider,
            Clock clock) {

        this.auditEventRepository = auditEventRepository;

        this.contextProvider = contextProvider;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(SecurityAuditRecord record) {

        if (record == null) {
            throw new IllegalArgumentException("Security audit record must not be null");
        }

        SecurityAuditContext context = contextProvider.currentContext();

        if (context == null || context.actor() == null) {

            throw new IllegalStateException("Security audit context must contain an actor");
        }

        SecurityAuditActor actor = context.actor();

        SecurityAuditEvent event =
                new SecurityAuditEvent(
                        record.eventType(),
                        actor.type(),
                        actor.reference(),
                        record.targetType(),
                        record.targetReference(),
                        record.outcome(),
                        context.correlationId(),
                        record.reason(),
                        record.metadata(),
                        OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        auditEventRepository.save(event);
    }
}
