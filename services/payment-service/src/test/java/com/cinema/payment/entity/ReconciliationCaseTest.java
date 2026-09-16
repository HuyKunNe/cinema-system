package com.cinema.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class ReconciliationCaseTest {

    private static final UUID PAYMENT_ID = UUID.fromString("01994111-1111-7111-8111-111111111111");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("01994111-2222-7222-8222-222222222222");

    private static final OffsetDateTime OPENED_AT = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    private static final OffsetDateTime RESOLVED_AT = OffsetDateTime.parse("2026-09-16T11:00:00Z");

    @Test
    void shouldCreateOpenReconciliationCase() {

        ReconciliationCase reconciliationCase = newCase();

        assertThat(reconciliationCase.getId()).isNotNull();

        assertThat(reconciliationCase.getPaymentId()).isEqualTo(PAYMENT_ID);

        assertThat(reconciliationCase.getPaymentTransactionId()).isEqualTo(TRANSACTION_ID);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        assertThat(reconciliationCase.getReason())
                .isEqualTo(ReconciliationReason.REFUND_PROVIDER_UNKNOWN);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getProvider()).isEqualTo("MOCK");

        assertThat(reconciliationCase.getProviderReference()).isEqualTo("refund-reference");

        assertThat(reconciliationCase.getOpenedAt()).isEqualTo(OPENED_AT);

        assertThat(reconciliationCase.getResolvedAt()).isNull();

        assertThat(reconciliationCase.getResolvedByType()).isNull();

        assertThat(reconciliationCase.getResolvedBy()).isNull();

        assertThat(reconciliationCase.getResolutionReason()).isNull();
    }

    @Test
    void shouldNormalizeProviderAndProviderReference() {

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        PAYMENT_ID,
                        TRANSACTION_ID,
                        ReconciliationReason.REFUND_PROVIDER_PENDING,
                        "  mock  ",
                        "  refund-reference  ",
                        OPENED_AT);

        assertThat(reconciliationCase.getProvider()).isEqualTo("MOCK");

        assertThat(reconciliationCase.getProviderReference()).isEqualTo("refund-reference");
    }

    @Test
    void blankProviderReferenceShouldBeStoredAsNull() {

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        PAYMENT_ID,
                        TRANSACTION_ID,
                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                        "MOCK",
                        "   ",
                        OPENED_AT);

        assertThat(reconciliationCase.getProviderReference()).isNull();
    }

    @Test
    void shouldResolveOpenCaseAsRefundSucceeded() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "finance-admin",
                "Provider dashboard confirms refund",
                RESOLVED_AT);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(reconciliationCase.getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        assertThat(reconciliationCase.getResolvedByType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(reconciliationCase.getResolvedBy()).isEqualTo("finance-admin");

        assertThat(reconciliationCase.getResolutionReason())
                .isEqualTo("Provider dashboard confirms refund");

        assertThat(reconciliationCase.getResolvedAt()).isEqualTo(RESOLVED_AT);
    }

    @Test
    void shouldResolveOpenCaseAsRefundFailed() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_FAILED,
                FinancialAuditActorType.SERVICE,
                "payment-reconciliation",
                "Provider confirms refund failure",
                RESOLVED_AT);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(reconciliationCase.getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_FAILED);

        assertThat(reconciliationCase.getResolvedByType())
                .isEqualTo(FinancialAuditActorType.SERVICE);

        assertThat(reconciliationCase.getResolvedBy()).isEqualTo("payment-reconciliation");

        assertThat(reconciliationCase.getResolutionReason())
                .isEqualTo("Provider confirms refund failure");

        assertThat(reconciliationCase.getResolvedAt()).isEqualTo(RESOLVED_AT);
    }

    @Test
    void shouldRejectOpenCase() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.reject(
                FinancialAuditActorType.USER,
                "finance-admin",
                "Case opened from invalid provider observation",
                RESOLVED_AT);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.REJECTED);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getResolvedByType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(reconciliationCase.getResolvedBy()).isEqualTo("finance-admin");

        assertThat(reconciliationCase.getResolutionReason())
                .isEqualTo("Case opened from invalid provider observation");

        assertThat(reconciliationCase.getResolvedAt()).isEqualTo(RESOLVED_AT);
    }

    @Test
    void resolutionReasonMayBeNull() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.SYSTEM,
                "payment-reconciliation",
                null,
                RESOLVED_AT);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(reconciliationCase.getResolutionReason()).isNull();
    }

    @Test
    void resolvedCaseShouldRejectAnotherResolveTransition() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "finance-admin",
                null,
                RESOLVED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        ReconciliationResolution.REFUND_FAILED,
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void resolvedCaseShouldRejectRejectTransition() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "finance-admin",
                null,
                RESOLVED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCase.reject(
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectedCaseShouldRejectResolveTransition() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.reject(FinancialAuditActorType.USER, "finance-admin", null, RESOLVED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        ReconciliationResolution.REFUND_SUCCEEDED,
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectedCaseShouldRejectAnotherRejectTransition() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.reject(FinancialAuditActorType.USER, "finance-admin", null, RESOLVED_AT);

        assertThatThrownBy(
                        () ->
                                reconciliationCase.reject(
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRequirePaymentId() {

        assertThatThrownBy(
                        () ->
                                new ReconciliationCase(
                                        null,
                                        TRANSACTION_ID,
                                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                                        "MOCK",
                                        null,
                                        OPENED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRequirePaymentTransactionId() {

        assertThatThrownBy(
                        () ->
                                new ReconciliationCase(
                                        PAYMENT_ID,
                                        null,
                                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                                        "MOCK",
                                        null,
                                        OPENED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRequireReconciliationReason() {

        assertThatThrownBy(
                        () ->
                                new ReconciliationCase(
                                        PAYMENT_ID, TRANSACTION_ID, null, "MOCK", null, OPENED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRequireProvider() {

        assertThatThrownBy(
                        () ->
                                new ReconciliationCase(
                                        PAYMENT_ID,
                                        TRANSACTION_ID,
                                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                                        " ",
                                        null,
                                        OPENED_AT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRequireOpenedAt() {

        assertThatThrownBy(
                        () ->
                                new ReconciliationCase(
                                        PAYMENT_ID,
                                        TRANSACTION_ID,
                                        ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                                        "MOCK",
                                        null,
                                        null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resolveShouldRequireResolution() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        null,
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void resolveShouldRequireActorType() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        ReconciliationResolution.REFUND_SUCCEEDED,
                                        null,
                                        "finance-admin",
                                        null,
                                        RESOLVED_AT))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void resolveShouldRequireActorId() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        ReconciliationResolution.REFUND_SUCCEEDED,
                                        FinancialAuditActorType.USER,
                                        " ",
                                        null,
                                        RESOLVED_AT))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void resolveShouldRequireResolvedAt() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.resolve(
                                        ReconciliationResolution.REFUND_SUCCEEDED,
                                        FinancialAuditActorType.USER,
                                        "finance-admin",
                                        null,
                                        null))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void rejectShouldRequireActorType() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () -> reconciliationCase.reject(null, "finance-admin", null, RESOLVED_AT))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void rejectShouldRequireActorId() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.reject(
                                        FinancialAuditActorType.USER, " ", null, RESOLVED_AT))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void rejectShouldRequireResolvedAt() {

        ReconciliationCase reconciliationCase = newCase();

        assertThatThrownBy(
                        () ->
                                reconciliationCase.reject(
                                        FinancialAuditActorType.USER, "finance-admin", null, null))
                .isInstanceOf(ValidationException.class);

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);
    }

    @Test
    void actorIdShouldBeNormalized() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "  finance-admin  ",
                null,
                RESOLVED_AT);

        assertThat(reconciliationCase.getResolvedBy()).isEqualTo("finance-admin");
    }

    @Test
    void resolutionReasonShouldBeNormalized() {

        ReconciliationCase reconciliationCase = newCase();

        reconciliationCase.resolve(
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                "finance-admin",
                "  Provider confirms refund  ",
                RESOLVED_AT);

        assertThat(reconciliationCase.getResolutionReason()).isEqualTo("Provider confirms refund");
    }

    private static ReconciliationCase newCase() {

        return new ReconciliationCase(
                PAYMENT_ID,
                TRANSACTION_ID,
                ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                "MOCK",
                "refund-reference",
                OPENED_AT);
    }
}
