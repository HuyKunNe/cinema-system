package com.cinema.user.security.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.repository.SecurityAuditEventRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
class SecurityAuditRecorderIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CORRELATION_ID = "request-security-audit-integration";

    @Autowired private SecurityAuditRecorder recorder;

    @Autowired private SecurityAuditEventRepository auditEventRepository;

    @Autowired private EntityManager entityManager;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @WithMockUser(username = "security-audit-admin", authorities = "user:manage")
    void shouldPersistAuthenticatedActorAuditEvent() {
        String targetReference = "target-user-" + UUID.randomUUID();

        MDC.put("correlationId", CORRELATION_ID);

        recorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                        SecurityAuditTargetType.USER,
                        targetReference,
                        SecurityAuditOutcome.SUCCESS,
                        "ADMINISTRATIVE_CHANGE",
                        "role=ADMIN"));

        flushAndClear();

        List<SecurityAuditEvent> events =
                auditEventRepository.findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                        SecurityAuditTargetType.USER, targetReference);

        assertThat(events)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType())
                                    .isEqualTo(SecurityAuditEventType.USER_ROLE_ASSIGNED);

                            assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

                            assertThat(event.getActorReference()).isEqualTo("security-audit-admin");

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.USER);

                            assertThat(event.getTargetReference()).isEqualTo(targetReference);

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);

                            assertThat(event.getReason()).isEqualTo("ADMINISTRATIVE_CHANGE");

                            assertThat(event.getMetadata()).isEqualTo("role=ADMIN");

                            assertThat(event.getOccurredAt()).isNotNull();

                            assertThat(event.getCreatedAt()).isNotNull();

                            assertThat(event.getUpdatedAt()).isNotNull();
                        });
    }

    @Test
    void shouldPersistSystemActorWhenAuthenticationIsMissing() {
        String targetReference = "inventory-service-" + UUID.randomUUID();

        recorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.OAUTH2_CLIENT_DEACTIVATED,
                        SecurityAuditTargetType.OAUTH2_CLIENT,
                        targetReference,
                        SecurityAuditOutcome.SUCCESS,
                        "CLIENT_DEACTIVATED",
                        null));

        flushAndClear();

        List<SecurityAuditEvent> events =
                auditEventRepository.findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                        SecurityAuditTargetType.OAUTH2_CLIENT, targetReference);

        assertThat(events)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getActorType())
                                    .isEqualTo(SecurityAuditActorType.SYSTEM);

                            assertThat(event.getActorReference()).isEqualTo(CommonConstants.SYSTEM);

                            assertThat(event.getCorrelationId()).isNull();

                            assertThat(event.getReason()).isEqualTo("CLIENT_DEACTIVATED");

                            assertThat(event.getMetadata()).isNull();
                        });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
