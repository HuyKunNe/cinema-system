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
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;
import com.cinema.payment.service.RefundRequestService;

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

@WebMvcTest(PaymentRefundController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentRefundControllerTest {

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("019c1234-3333-7abc-8def-0123456789ab");

    private static final UUID CORRELATION_ID =
            UUID.fromString("019c1234-4444-7abc-8def-0123456789ab");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RefundRequestService refundRequestService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void refundShouldMapAuthenticatedPrincipalAndRequestToService() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        when(refundRequestService.requestRefund(any(RefundRequest.class)))
                .thenReturn(
                        new RefundRequestResult(
                                PAYMENT_ID, TRANSACTION_ID, RefundStatus.PENDING, false));

        OffsetDateTime beforeRequest = OffsetDateTime.now(ZoneOffset.UTC);

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "correlationId": "%s",
                                          "reason": "Customer requested refund"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.refundStatus").value("PENDING"))
                .andExpect(jsonPath("$.duplicate").value(false));

        OffsetDateTime afterRequest = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<RefundRequest> requestCaptor = ArgumentCaptor.forClass(RefundRequest.class);

        verify(refundRequestService).requestRefund(requestCaptor.capture());

        RefundRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(capturedRequest.actorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(capturedRequest.actorId()).isEqualTo("admin-123");

        assertThat(capturedRequest.reason()).isEqualTo("Customer requested refund");

        assertThat(capturedRequest.correlationId()).isEqualTo(CORRELATION_ID);

        assertThat(capturedRequest.requestedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeRequest)
                .isBeforeOrEqualTo(afterRequest);

        assertThat(capturedRequest.requestedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void refundShouldReturnDuplicateResult() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        when(refundRequestService.requestRefund(any(RefundRequest.class)))
                .thenReturn(
                        new RefundRequestResult(
                                PAYMENT_ID, TRANSACTION_ID, RefundStatus.PENDING, true));

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "correlationId": "%s",
                                          "reason": "Customer requested refund"
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.refundStatus").value("PENDING"))
                .andExpect(jsonPath("$.duplicate").value(true));

        verify(refundRequestService).requestRefund(any(RefundRequest.class));
    }

    @Test
    void refundShouldRejectMissingCorrelationId() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "reason": "Customer requested refund"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundRequestService);
    }

    @Test
    void refundShouldRejectBlankReason() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin-123", "n/a");

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "correlationId": "%s",
                                          "reason": "   "
                                        }
                                        """
                                                .formatted(CORRELATION_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundRequestService);
    }
}
