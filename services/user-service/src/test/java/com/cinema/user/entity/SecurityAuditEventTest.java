package com.cinema.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

class SecurityAuditEventTest {

    private static final String ACTOR_REFERENCE = "platform-admin";

    private static final String TARGET_REFERENCE = "target-user";

    private static final String CORRELATION_ID = "request-019c6000";

    private static final String REASON = "ADMINISTRATIVE_CHANGE";

    private static final String METADATA = "source=administrative-service";

    private static final OffsetDateTime OCCURRED_AT =
            OffsetDateTime.of(2026, 8, 17, 11, 30, 0, 0, ZoneOffset.UTC);

    @Test
    void validEventShouldPreserveAuditData() {
        SecurityAuditEvent event = createValidEvent();

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getEventType()).isEqualTo(SecurityAuditEventType.USER_ROLE_ASSIGNED);

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(event.getActorReference()).isEqualTo(ACTOR_REFERENCE);

        assertThat(event.getTargetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(event.getTargetReference()).isEqualTo(TARGET_REFERENCE);

        assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

        assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);

        assertThat(event.getReason()).isEqualTo(REASON);

        assertThat(event.getMetadata()).isEqualTo(METADATA);

        assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void constructorShouldTrimTextValues() {
        SecurityAuditEvent event =
                new SecurityAuditEvent(
                        SecurityAuditEventType.OAUTH2_CLIENT_REGISTERED,
                        SecurityAuditActorType.USER,
                        "  platform-admin  ",
                        SecurityAuditTargetType.OAUTH2_CLIENT,
                        "  inventory-service  ",
                        SecurityAuditOutcome.SUCCESS,
                        "  request-123  ",
                        "  CLIENT_CREATED  ",
                        "  clientType=service  ",
                        OCCURRED_AT);

        assertThat(event.getActorReference()).isEqualTo("platform-admin");

        assertThat(event.getTargetReference()).isEqualTo("inventory-service");

        assertThat(event.getCorrelationId()).isEqualTo("request-123");

        assertThat(event.getReason()).isEqualTo("CLIENT_CREATED");

        assertThat(event.getMetadata()).isEqualTo("clientType=service");
    }

    @Test
    void eventWithoutTargetOrOptionalContextShouldBeAllowed() {
        SecurityAuditEvent event =
                new SecurityAuditEvent(
                        SecurityAuditEventType.AUTHENTICATION_FAILED,
                        SecurityAuditActorType.SYSTEM,
                        "SYSTEM",
                        null,
                        null,
                        SecurityAuditOutcome.FAILURE,
                        null,
                        null,
                        null,
                        OCCURRED_AT);

        assertThat(event.getTargetType()).isNull();

        assertThat(event.getTargetReference()).isNull();

        assertThat(event.getCorrelationId()).isNull();

        assertThat(event.getReason()).isNull();

        assertThat(event.getMetadata()).isNull();
    }

    @Test
    void nullEventTypeShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        null,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullActorTypeShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        null,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankActorReferenceShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        "   ",
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void targetTypeWithoutReferenceShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        null,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void targetReferenceWithoutTypeShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        null,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankTargetReferenceShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        "   ",
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullOutcomeShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        null,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullOccurredAtShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankOptionalTextShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        "   ",
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void oversizedActorReferenceShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        "a".repeat(201),
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void oversizedTargetReferenceShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        "t".repeat(201),
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void oversizedCorrelationIdShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        "c".repeat(101),
                                        REASON,
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void oversizedReasonShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        "r".repeat(101),
                                        METADATA,
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void oversizedMetadataShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new SecurityAuditEvent(
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditActorType.USER,
                                        ACTOR_REFERENCE,
                                        SecurityAuditTargetType.USER,
                                        TARGET_REFERENCE,
                                        SecurityAuditOutcome.SUCCESS,
                                        CORRELATION_ID,
                                        REASON,
                                        "m".repeat(1001),
                                        OCCURRED_AT))
                .isInstanceOf(ValidationException.class);
    }

    private SecurityAuditEvent createValidEvent() {
        return new SecurityAuditEvent(
                SecurityAuditEventType.USER_ROLE_ASSIGNED,
                SecurityAuditActorType.USER,
                ACTOR_REFERENCE,
                SecurityAuditTargetType.USER,
                TARGET_REFERENCE,
                SecurityAuditOutcome.SUCCESS,
                CORRELATION_ID,
                REASON,
                METADATA,
                OCCURRED_AT);
    }
}
