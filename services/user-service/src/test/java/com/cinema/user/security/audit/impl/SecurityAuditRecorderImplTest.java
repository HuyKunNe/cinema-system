package com.cinema.user.security.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.security.audit.SecurityAuditActor;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditContext;
import com.cinema.user.security.audit.SecurityAuditContextProvider;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ExtendWith(MockitoExtension.class)
class SecurityAuditRecorderImplTest {

    private static final String ACTOR_REFERENCE = "security-audit-admin";

    private static final String TARGET_REFERENCE = "target-user";

    private static final String CORRELATION_ID = "request-019c5000";

    private static final String REASON = "ADMINISTRATIVE_CHANGE";

    private static final String METADATA = "source=administrative-service";

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-17T10:30:00Z");

    private static final OffsetDateTime FIXED_TIME =
            OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock private SecurityAuditEventRepository auditEventRepository;

    @Mock private SecurityAuditContextProvider contextProvider;

    @Captor private ArgumentCaptor<SecurityAuditEvent> eventCaptor;

    private SecurityAuditRecorderImpl recorder;

    @BeforeEach
    void setUp() {
        recorder =
                new SecurityAuditRecorderImpl(auditEventRepository, contextProvider, FIXED_CLOCK);
    }

    @Test
    void recordShouldPersistResolvedContextAndAuditData() {
        when(contextProvider.currentContext())
                .thenReturn(
                        new SecurityAuditContext(
                                new SecurityAuditActor(
                                        SecurityAuditActorType.USER, ACTOR_REFERENCE),
                                CORRELATION_ID));

        recorder.record(validRecord());

        verify(auditEventRepository).save(eventCaptor.capture());

        SecurityAuditEvent event = eventCaptor.getValue();

        assertThat(event.getEventType()).isEqualTo(SecurityAuditEventType.USER_ROLE_ASSIGNED);

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(event.getActorReference()).isEqualTo(ACTOR_REFERENCE);

        assertThat(event.getTargetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(event.getTargetReference()).isEqualTo(TARGET_REFERENCE);

        assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

        assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);

        assertThat(event.getReason()).isEqualTo(REASON);

        assertThat(event.getMetadata()).isEqualTo(METADATA);

        assertThat(event.getOccurredAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    void recordShouldPersistSystemActorWithoutOptionalContext() {
        when(contextProvider.currentContext())
                .thenReturn(
                        new SecurityAuditContext(
                                new SecurityAuditActor(SecurityAuditActorType.SYSTEM, "SYSTEM"),
                                null));

        recorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.AUTHENTICATION_FAILED,
                        null,
                        null,
                        SecurityAuditOutcome.FAILURE,
                        null,
                        null));

        verify(auditEventRepository).save(eventCaptor.capture());

        SecurityAuditEvent event = eventCaptor.getValue();

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.SYSTEM);

        assertThat(event.getActorReference()).isEqualTo("SYSTEM");

        assertThat(event.getTargetType()).isNull();

        assertThat(event.getTargetReference()).isNull();

        assertThat(event.getCorrelationId()).isNull();

        assertThat(event.getReason()).isNull();

        assertThat(event.getMetadata()).isNull();

        assertThat(event.getOccurredAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    void nullRecordShouldBeRejectedBeforeContextResolution() {
        assertThatThrownBy(() -> recorder.record(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Security audit record must not be null");

        verifyNoInteractions(contextProvider, auditEventRepository);
    }

    @Test
    void nullContextShouldBeRejected() {
        when(contextProvider.currentContext()).thenReturn(null);

        assertThatThrownBy(() -> recorder.record(validRecord()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Security audit context must contain an actor");

        verifyNoInteractions(auditEventRepository);
    }

    @Test
    void contextWithoutActorShouldBeRejected() {
        when(contextProvider.currentContext())
                .thenReturn(new SecurityAuditContext(null, CORRELATION_ID));

        assertThatThrownBy(() -> recorder.record(validRecord()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Security audit context must contain an actor");

        verifyNoInteractions(auditEventRepository);
    }

    private static SecurityAuditRecord validRecord() {
        return new SecurityAuditRecord(
                SecurityAuditEventType.USER_ROLE_ASSIGNED,
                SecurityAuditTargetType.USER,
                TARGET_REFERENCE,
                SecurityAuditOutcome.SUCCESS,
                REASON,
                METADATA);
    }
}
