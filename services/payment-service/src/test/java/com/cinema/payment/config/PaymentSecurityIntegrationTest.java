package com.cinema.payment.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.payment.controller.PaymentAuditController;
import com.cinema.payment.controller.PaymentProviderWebhookController;
import com.cinema.payment.controller.PaymentReconciliationController;
import com.cinema.payment.controller.PaymentRefundController;
import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.service.FinancialAuditReadService;
import com.cinema.payment.service.PaymentProviderWebhookHandlingService;
import com.cinema.payment.service.ReconciliationAdminService;
import com.cinema.payment.service.RefundRequestService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(
        controllers = {
            PaymentSecurityIntegrationTest.PaymentSecurityProbeController.class,
            PaymentProviderWebhookController.class,
            PaymentRefundController.class,
            PaymentReconciliationController.class,
            PaymentAuditController.class
        })
@ActiveProfiles("test")
@Import({
    PaymentSecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class,
    PaymentSecurityIntegrationTest.PaymentSecurityProbeController.class
})
class PaymentSecurityIntegrationTest {

    private static final UUID AUDIT_ID = UUID.fromString("019c1234-1111-7abc-8def-0123456789ab");

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("019c1234-3333-7abc-8def-0123456789ab");

    private static final UUID CORRELATION_ID =
            UUID.fromString("019c1234-4444-7abc-8def-0123456789ab");

    private static final UUID RECONCILIATION_CASE_ID =
            UUID.fromString("019c1234-5555-7abc-8def-0123456789ab");

    private static final OffsetDateTime RESOLVED_AT = OffsetDateTime.parse("2026-09-17T04:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean private PaymentProviderWebhookHandlingService webhookHandlingService;

    @MockitoBean private RefundRequestService refundRequestService;

    @MockitoBean private ReconciliationAdminService reconciliationAdminService;

    @MockitoBean private FinancialAuditReadService financialAuditReadService;

    @Test
    void unauthenticatedPaymentQueryShouldBeRejected() throws Exception {

        mockMvc.perform(get("/api/v1/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void paymentQueryWithoutPaymentReadShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "booking:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));
    }

    @Test
    void paymentQueryWithPaymentReadShouldBeAllowed() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:read"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedRefundShouldBeRejected() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRefundRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verify(refundRequestService, never()).requestRefund(any(RefundRequest.class));
    }

    @Test
    void refundWithoutPaymentRefundAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority("payment:read")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRefundRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(refundRequestService, never()).requestRefund(any(RefundRequest.class));
    }

    @Test
    void refundWithPaymentReconcileAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:reconcile")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRefundRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(refundRequestService, never()).requestRefund(any(RefundRequest.class));
    }

    @Test
    void refundWithPaymentRefundAuthorityShouldBeAllowed() throws Exception {

        when(refundRequestService.requestRefund(any(RefundRequest.class)))
                .thenReturn(
                        new RefundRequestResult(
                                PAYMENT_ID, TRANSACTION_ID, RefundStatus.PENDING, false));

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:refund")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRefundRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.refundStatus").value("PENDING"))
                .andExpect(jsonPath("$.duplicate").value(false));

        verify(refundRequestService).requestRefund(any(RefundRequest.class));
    }

    @Test
    void unauthenticatedReconciliationResolveShouldBeRejected() throws Exception {

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/resolve",
                                        RECONCILIATION_CASE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validResolveRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verify(reconciliationAdminService, never())
                .resolve(any(ReconciliationResolveRequest.class));
    }

    @Test
    void reconciliationResolveWithoutPaymentReconcileAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/resolve",
                                        RECONCILIATION_CASE_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:refund")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validResolveRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(reconciliationAdminService, never())
                .resolve(any(ReconciliationResolveRequest.class));
    }

    @Test
    void reconciliationResolveWithPaymentReconcileAuthorityShouldBeAllowed() throws Exception {

        when(reconciliationAdminService.resolve(any(ReconciliationResolveRequest.class)))
                .thenReturn(
                        new ReconciliationOperationResult(
                                RECONCILIATION_CASE_ID,
                                PAYMENT_ID,
                                TRANSACTION_ID,
                                ReconciliationStatus.RESOLVED,
                                ReconciliationResolution.REFUND_SUCCEEDED,
                                RefundStatus.SUCCEEDED,
                                PaymentTransactionStatus.SUCCEEDED,
                                RESOLVED_AT));

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/resolve",
                                        RECONCILIATION_CASE_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:reconcile")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validResolveRequestBody()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.reconciliationCaseId").value(RECONCILIATION_CASE_ID.toString()))
                .andExpect(jsonPath("$.reconciliationStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("REFUND_SUCCEEDED"));

        verify(reconciliationAdminService).resolve(any(ReconciliationResolveRequest.class));
    }

    @Test
    void unauthenticatedReconciliationRejectShouldBeRejected() throws Exception {

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/reject",
                                        RECONCILIATION_CASE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRejectRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verify(reconciliationAdminService, never()).reject(any(ReconciliationRejectRequest.class));
    }

    @Test
    void reconciliationRejectWithoutPaymentReconcileAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/reject",
                                        RECONCILIATION_CASE_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:refund")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRejectRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(reconciliationAdminService, never()).reject(any(ReconciliationRejectRequest.class));
    }

    @Test
    void reconciliationRejectWithPaymentReconcileAuthorityShouldBeAllowed() throws Exception {

        when(reconciliationAdminService.reject(any(ReconciliationRejectRequest.class)))
                .thenReturn(
                        new ReconciliationOperationResult(
                                RECONCILIATION_CASE_ID,
                                PAYMENT_ID,
                                TRANSACTION_ID,
                                ReconciliationStatus.REJECTED,
                                null,
                                RefundStatus.PENDING,
                                PaymentTransactionStatus.PENDING_PROVIDER,
                                RESOLVED_AT));

        mockMvc.perform(
                        post(
                                        "/api/v1/payments/reconciliation-cases/{caseId}/reject",
                                        RECONCILIATION_CASE_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:reconcile")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRejectRequestBody()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.reconciliationCaseId").value(RECONCILIATION_CASE_ID.toString()))
                .andExpect(jsonPath("$.reconciliationStatus").value("REJECTED"));

        verify(reconciliationAdminService).reject(any(ReconciliationRejectRequest.class));
    }

    @Test
    void unauthenticatedPaymentAuditShouldBeRejected() throws Exception {

        mockMvc.perform(get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verify(financialAuditReadService, never()).findByPaymentId(any(UUID.class));
    }

    @Test
    void paymentAuditWithPaymentReadAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(financialAuditReadService, never()).findByPaymentId(any(UUID.class));
    }

    @Test
    void paymentAuditWithPaymentRefundAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:refund"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(financialAuditReadService, never()).findByPaymentId(any(UUID.class));
    }

    @Test
    void paymentAuditWithPaymentReconcileAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:reconcile"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verify(financialAuditReadService, never()).findByPaymentId(any(UUID.class));
    }

    @Test
    void paymentAuditWithPaymentAuditAuthorityShouldBeAllowed() throws Exception {

        when(financialAuditReadService.findByPaymentId(PAYMENT_ID))
                .thenReturn(
                        List.of(
                                new FinancialAuditRecordResponse(
                                        AUDIT_ID,
                                        PAYMENT_ID,
                                        FinancialAuditAction.REFUND_REQUESTED,
                                        FinancialAuditActorType.USER,
                                        "admin-123",
                                        "Customer requested refund",
                                        null,
                                        CORRELATION_ID,
                                        OffsetDateTime.parse("2026-09-17T03:30:00Z"))));

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID)
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("auditor-123"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:audit"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(AUDIT_ID.toString()))
                .andExpect(jsonPath("$[0].paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$[0].action").value("REFUND_REQUESTED"));

        verify(financialAuditReadService).findByPaymentId(PAYMENT_ID);
    }

    @Test
    void unauthenticatedProviderWebhookShouldBeAllowed() throws Exception {

        when(webhookHandlingService.handle(eq("MOMO"), anyMap(), any(byte[].class)))
                .thenReturn(ProviderWebhookAcknowledgement.noContent());

        mockMvc.perform(
                        post("/api/v1/payments/webhooks/MOMO")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isNoContent());
    }

    private static String validRefundRequestBody() {

        return """
        {
          "correlationId": "%s",
          "reason": "Customer requested refund"
        }
        """
                .formatted(CORRELATION_ID);
    }

    private static String validResolveRequestBody() {

        return """
        {
          "resolution": "REFUND_SUCCEEDED",
          "reason": "Provider confirmed refund",
          "providerReference": "refund-provider-123",
          "correlationId": "%s"
        }
        """
                .formatted(CORRELATION_ID);
    }

    private static String validRejectRequestBody() {

        return """
        {
          "reason": "Insufficient provider evidence",
          "correlationId": "%s"
        }
        """
                .formatted(CORRELATION_ID);
    }

    @RestController
    @RequestMapping("/api/v1/payments")
    static class PaymentSecurityProbeController {

        @GetMapping("/{paymentId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void findById(@PathVariable("paymentId") UUID paymentId) {}
    }
}
