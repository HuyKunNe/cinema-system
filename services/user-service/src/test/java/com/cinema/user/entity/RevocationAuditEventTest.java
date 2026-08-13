package com.cinema.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;

class RevocationAuditEventTest {

    private static final UUID ACTOR_USER_ID =
            UUID.fromString(
                    "019c5000-0000-7000-8000-000000000701");

    private static final String TARGET_REFERENCE =
            "target-user";

    private static final String ACTOR_NAME =
            "platform-admin";

    private static final int REVOKED_AUTHORIZATION_COUNT =
            3;

    private static final OffsetDateTime OCCURRED_AT =
            OffsetDateTime.of(
                    2026,
                    8,
                    13,
                    10,
                    30,
                    0,
                    0,
                    ZoneOffset.UTC);

    @Test
    void validEventShouldPreserveAuditData() {
        RevocationAuditEvent event =
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        REVOKED_AUTHORIZATION_COUNT,
                        OCCURRED_AT);

        assertThat(event.getId())
                .isNotNull();

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
                        REVOKED_AUTHORIZATION_COUNT);

        assertThat(event.getOccurredAt())
                .isEqualTo(
                        OCCURRED_AT);
    }

    @Test
    void constructorShouldTrimTextValues() {
        RevocationAuditEvent event =
                new RevocationAuditEvent(
                        RevocationAuditTargetType.CLIENT,
                        "  inventory-service  ",
                        RevocationReason.CLIENT_DEACTIVATED,
                        ACTOR_USER_ID,
                        "  platform-admin  ",
                        1,
                        OCCURRED_AT);

        assertThat(event.getTargetReference())
                .isEqualTo(
                        "inventory-service");

        assertThat(event.getActorName())
                .isEqualTo(
                        "platform-admin");
    }

    @Test
    void nullTargetTypeShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        null,
                        TARGET_REFERENCE,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        REVOKED_AUTHORIZATION_COUNT,
                        OCCURRED_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void blankTargetReferenceShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        "   ",
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        REVOKED_AUTHORIZATION_COUNT,
                        OCCURRED_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void nullReasonShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        null,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        REVOKED_AUTHORIZATION_COUNT,
                        OCCURRED_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void blankActorNameShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        "   ",
                        REVOKED_AUTHORIZATION_COUNT,
                        OCCURRED_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void negativeRevokedAuthorizationCountShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        -1,
                        OCCURRED_AT))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void nullOccurredAtShouldBeRejected() {
        assertThatThrownBy(() ->
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        ACTOR_USER_ID,
                        ACTOR_NAME,
                        REVOKED_AUTHORIZATION_COUNT,
                        null))
                .isInstanceOf(
                        ValidationException.class);
    }

    @Test
    void systemActorShouldAllowNullActorUserId() {
        RevocationAuditEvent event =
                new RevocationAuditEvent(
                        RevocationAuditTargetType.USER,
                        TARGET_REFERENCE,
                        RevocationReason.ACCOUNT_LOCKED,
                        null,
                        "SYSTEM",
                        0,
                        OCCURRED_AT);

        assertThat(event.getActorUserId())
                .isNull();

        assertThat(event.getActorName())
                .isEqualTo(
                        "SYSTEM");

        assertThat(event.getRevokedAuthorizationCount())
                .isZero();
    }
}
