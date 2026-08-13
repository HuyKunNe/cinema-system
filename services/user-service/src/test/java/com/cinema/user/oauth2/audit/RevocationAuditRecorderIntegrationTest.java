package com.cinema.user.oauth2.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RevocationAuditEvent;
import com.cinema.user.repository.RevocationAuditEventRepository;

import jakarta.persistence.EntityManager;

@Transactional
class RevocationAuditRecorderIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private RevocationAuditRecorder recorder;

    @Autowired
    private RevocationAuditEventRepository auditEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @WithMockUser(username = "audit-admin", authorities = "user:manage")
    void shouldPersistAuthenticatedActorAuditEvent() {
        String targetReference = "inventory-service-"
                + UUID.randomUUID();

        recorder.record(
                RevocationAuditTargetType.CLIENT,
                targetReference,
                RevocationReason.ADMIN_CLIENT_REVOCATION,
                2);

        flushAndClear();

        List<RevocationAuditEvent> events = auditEventRepository
                .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                        RevocationAuditTargetType.CLIENT,
                        targetReference);

        assertThat(events)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTargetType())
                            .isEqualTo(
                                    RevocationAuditTargetType.CLIENT);

                    assertThat(event.getTargetReference())
                            .isEqualTo(
                                    targetReference);

                    assertThat(event.getReason())
                            .isEqualTo(
                                    RevocationReason.ADMIN_CLIENT_REVOCATION);

                    assertThat(event.getActorUserId())
                            .isNull();

                    assertThat(event.getActorName())
                            .isEqualTo(
                                    "audit-admin");

                    assertThat(event.getRevokedAuthorizationCount())
                            .isEqualTo(
                                    2);

                    assertThat(event.getOccurredAt())
                            .isNotNull();

                    assertThat(event.getCreatedAt())
                            .isNotNull();

                    assertThat(event.getUpdatedAt())
                            .isNotNull();
                });
    }

    @Test
    void shouldPersistSystemActorWhenAuthenticationIsMissing() {
        String targetReference = "system-user-"
                + UUID.randomUUID();

        recorder.record(
                RevocationAuditTargetType.USER,
                targetReference,
                RevocationReason.ACCOUNT_LOCKED,
                1);

        flushAndClear();

        List<RevocationAuditEvent> events = auditEventRepository
                .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                        RevocationAuditTargetType.USER,
                        targetReference);

        assertThat(events)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getActorUserId())
                            .isNull();

                    assertThat(event.getActorName())
                            .isEqualTo(
                                    CommonConstants.SYSTEM);

                    assertThat(event.getReason())
                            .isEqualTo(
                                    RevocationReason.ACCOUNT_LOCKED);
                });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
