package com.cinema.payment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.service.FinancialAuditReadService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(PaymentAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentAuditControllerTest {

    private static final UUID AUDIT_ID = UUID.fromString("019c1234-1111-7abc-8def-0123456789ab");

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    private static final UUID CORRELATION_ID =
            UUID.fromString("019c1234-4444-7abc-8def-0123456789ab");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FinancialAuditReadService financialAuditReadService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void auditShouldReturnPaymentAuditRecords() throws Exception {

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

        mockMvc.perform(get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(AUDIT_ID.toString()))
                .andExpect(jsonPath("$[0].paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$[0].action").value("REFUND_REQUESTED"))
                .andExpect(jsonPath("$[0].actorType").value("USER"))
                .andExpect(jsonPath("$[0].actorId").value("admin-123"))
                .andExpect(jsonPath("$[0].correlationId").value(CORRELATION_ID.toString()));
    }

    @Test
    void auditShouldReturnEmptyArrayWhenPaymentHasNoAuditRecords() throws Exception {

        when(financialAuditReadService.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payments/{paymentId}/audit", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
