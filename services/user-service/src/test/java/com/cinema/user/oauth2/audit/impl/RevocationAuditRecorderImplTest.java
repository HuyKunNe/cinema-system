package com.cinema.user.oauth2.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.user.entity.RevocationAuditEvent;
import com.cinema.user.oauth2.audit.RevocationAuditActor;
import com.cinema.user.oauth2.audit.RevocationAuditActorProvider;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.RevocationAuditEventRepository;

@ExtendWith(MockitoExtension.class)
class RevocationAuditRecorderImplTest {

    private static final UUID ACTOR_USER_ID = UUID.fromString(
            "019c5000-0000-7000-8000-000000000722");

    private static final String ACTOR_NAME = "audit-admin";

    private static final String TARGET_REFERENCE = "target-user";

    private static final Instant FIXED_INSTANT = Instant.parse(
            "2026-08-13T10:30:00Z");

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    @Mock
    private RevocationAuditEventRepository auditEventRepository;

    @Mock
    private RevocationAuditActorProvider actorProvider;

    @Captor
    private ArgumentCaptor<RevocationAuditEvent> eventCaptor;

    private RevocationAuditRecorderImpl recorder;

    @BeforeEach
    void setUp() {
        recorder = new RevocationAuditRecorderImpl(
                auditEventRepository,
                actorProvider,
                FIXED_CLOCK);
    }

    @Test
    void recordShouldPersistResolvedActorAndAuditData() {
        when(actorProvider.currentActor())
                .thenReturn(
                        new RevocationAuditActor(
                                ACTOR_USER_ID,
                                ACTOR_NAME));

        recorder.record(
                RevocationAuditTargetType.USER,
                TARGET_REFERENCE,
                RevocationReason.ADMIN_USER_REVOCATION,
                3);

        verify(auditEventRepository)
                .save(
                        eventCaptor.capture());

        RevocationAuditEvent event = eventCaptor.getValue();

        assertThat(event.getTargetType())
                .isEqualTo(
                        RevocationAuditTargetType.USER);

        assertThat(event.getTargetReference())
                .isEqualTo(
                        TARGET_REFERENCE);

        assertThat(event.getReason())
                .isEqualTo(
                        RevocationReason.ADMIN_USER_REVOCATION);

        assertThat(event.getActorUserId())
                .isEqualTo(
                        ACTOR_USER_ID);

        assertThat(event.getActorName())
                .isEqualTo(
                        ACTOR_NAME);

        assertThat(event.getRevokedAuthorizationCount())
                .isEqualTo(
                        3);

        assertThat(event.getOccurredAt())
                .isEqualTo(
                        FIXED_TIME);
    }

    @Test
    void recordShouldPersistSystemActorWithoutUserId() {
        when(actorProvider.currentActor())
                .thenReturn(
                        new RevocationAuditActor(
                                null,
                                CommonConstants.SYSTEM));

        recorder.record(
                RevocationAuditTargetType.CLIENT,
                "inventory-service",
                RevocationReason.CLIENT_DEACTIVATED,
                0);

        verify(auditEventRepository)
                .save(
                        eventCaptor.capture());

        RevocationAuditEvent event = eventCaptor.getValue();

        assertThat(event.getTargetType())
                .isEqualTo(
                        RevocationAuditTargetType.CLIENT);

        assertThat(event.getActorUserId())
                .isNull();

        assertThat(event.getActorName())
                .isEqualTo(
                        CommonConstants.SYSTEM);

        assertThat(event.getRevokedAuthorizationCount())
                .isZero();

        assertThat(event.getOccurredAt())
                .isEqualTo(
                        FIXED_TIME);
    }
}
