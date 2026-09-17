package com.cinema.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.service.ReconciliationAdminService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@WebMvcTest(PaymentReconciliationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentReconciliationControllerTest {

    private static final UUID CASE_ID = UUID.fromString("019c1234-5555-7abc-8def-0123456789ab");

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("019c1234-3333-7abc-8def-0123456789ab");

    private static final UUID CORRELATION_ID =
            UUID.fromString("019c1234-4444-7abc-8def-0123456789ab");

    private static final OffsetDateTime RESOLVED_AT = OffsetDateTime.parse("2026-09-17T04:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReconciliationAdminService reconciliationAdminService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void resolveShouldMapAuthenticatedPrincipalAndRequestToService() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        when(reconciliationAdminService.resolve(any(ReconciliationResolveRequest.class)))
                .thenReturn(
                        new ReconciliationOperationResult(
                                CASE_ID,
                                PAYMENT_ID,
                                TRANSACTION_ID,
                                ReconciliationStatus.RESOLVED,
                                ReconciliationResolution.REFUND_SUCCEEDED,
                                RefundStatus.SUCCEEDED,
                                PaymentTransactionStatus.SUCCEEDED,
                                RESOLVED_AT));

        OffsetDateTime beforeRequest = OffsetDateTime.now(ZoneOffset.UTC);

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/resolve", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "resolution": "REFUND_SUCCEEDED",
                                          "reason": "Provider confirmed refund",
                                          "providerReference": "refund-provider-123",
                                          "failureCode": null,
                                          "failureMessage": null,
                                          "correlationId": "%s"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationCaseId").value(CASE_ID.toString()))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.paymentTransactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.reconciliationStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("REFUND_SUCCEEDED"))
                .andExpect(jsonPath("$.refundStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.transactionStatus").value("SUCCEEDED"));

        OffsetDateTime afterRequest = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<ReconciliationResolveRequest> requestCaptor =
                ArgumentCaptor.forClass(ReconciliationResolveRequest.class);

        verify(reconciliationAdminService).resolve(requestCaptor.capture());

        ReconciliationResolveRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.reconciliationCaseId()).isEqualTo(CASE_ID);

        assertThat(capturedRequest.resolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        assertThat(capturedRequest.actorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(capturedRequest.actorId()).isEqualTo("admin-123");

        assertThat(capturedRequest.reason()).isEqualTo("Provider confirmed refund");

        assertThat(capturedRequest.providerReference()).isEqualTo("refund-provider-123");

        assertThat(capturedRequest.failureCode()).isNull();

        assertThat(capturedRequest.failureMessage()).isNull();

        assertThat(capturedRequest.correlationId()).isEqualTo(CORRELATION_ID);

        assertThat(capturedRequest.requestedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeRequest)
                .isBeforeOrEqualTo(afterRequest);

        assertThat(capturedRequest.requestedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void rejectShouldMapAuthenticatedPrincipalAndRequestToService() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-456", "n/a");

        when(reconciliationAdminService.reject(any(ReconciliationRejectRequest.class)))
                .thenReturn(
                        new ReconciliationOperationResult(
                                CASE_ID,
                                PAYMENT_ID,
                                TRANSACTION_ID,
                                ReconciliationStatus.REJECTED,
                                null,
                                RefundStatus.PENDING,
                                PaymentTransactionStatus.PENDING_PROVIDER,
                                RESOLVED_AT));

        OffsetDateTime beforeRequest = OffsetDateTime.now(ZoneOffset.UTC);

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/reject", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": "Insufficient provider evidence",
                                          "correlationId": "%s"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationCaseId").value(CASE_ID.toString()))
                .andExpect(jsonPath("$.reconciliationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.resolution").doesNotExist())
                .andExpect(jsonPath("$.refundStatus").value("PENDING"))
                .andExpect(jsonPath("$.transactionStatus").value("PENDING_PROVIDER"));

        OffsetDateTime afterRequest = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<ReconciliationRejectRequest> requestCaptor =
                ArgumentCaptor.forClass(ReconciliationRejectRequest.class);

        verify(reconciliationAdminService).reject(requestCaptor.capture());

        ReconciliationRejectRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.reconciliationCaseId()).isEqualTo(CASE_ID);

        assertThat(capturedRequest.actorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(capturedRequest.actorId()).isEqualTo("admin-456");

        assertThat(capturedRequest.reason()).isEqualTo("Insufficient provider evidence");

        assertThat(capturedRequest.correlationId()).isEqualTo(CORRELATION_ID);

        assertThat(capturedRequest.requestedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeRequest)
                .isBeforeOrEqualTo(afterRequest);

        assertThat(capturedRequest.requestedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void resolveShouldRejectMissingResolution() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/resolve", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": "Provider confirmed refund",
                                          "correlationId": "%s"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationAdminService);
    }

    @Test
    void resolveShouldRejectBlankReason() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/resolve", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "resolution": "REFUND_SUCCEEDED",
                                          "reason": "   ",
                                          "correlationId": "%s"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationAdminService);
    }

    @Test
    void rejectShouldRejectMissingCorrelationId() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/reject", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": "Insufficient provider evidence"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationAdminService);
    }

    @Test
    void rejectShouldRejectBlankReason() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/reconciliation-cases/{caseId}/reject", CASE_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": "   ",
                                          "correlationId": "%s"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationAdminService);
    }
}
