package com.cinema.payment.controller;

import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.payment.controller.response.PaymentReconciliationCaseResponse;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.service.ReconciliationManagementReadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments/management/reconciliation-cases")
@SecurityRequirement(name = "oauth2", scopes = {"payment:read"})
@Tag(name = "Payment reconciliation management")
public class PaymentReconciliationManagementController {

    private final ReconciliationManagementReadService reconciliationManagementReadService;

    public PaymentReconciliationManagementController(
            ReconciliationManagementReadService reconciliationManagementReadService) {

        this.reconciliationManagementReadService = reconciliationManagementReadService;
    }

    @Operation(
            operationId = "listPaymentReconciliationCasesForAdmin",
            summary = "List payment reconciliation cases for administrators",
            description =
                    "Requires the ADMIN role and payment:read permission. "
                            + "Optional filters are combined with AND. "
                            + "Omitting status returns all statuses. "
                            + "Results are ordered by openedAt ascending, then id ascending.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentReconciliationCaseResponse>>>
            listPaymentReconciliationCasesForAdmin(
                    @RequestParam(name = "status", required = false)
                            ReconciliationStatus status,
                    @RequestParam(name = "paymentId", required = false)
                            @UuidV7
                            UUID paymentId,
                    @RequestParam(name = "page", defaultValue = "0")
                            @Min(0)
                            int page,
                    @RequestParam(name = "size", defaultValue = "20")
                            @Min(1)
                            @Max(100)
                            int size) {

        PageResponse<PaymentReconciliationCaseResponse> response =
                reconciliationManagementReadService.findAll(
                        status, paymentId, page, size);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }

    @Operation(
            operationId = "getPaymentReconciliationCaseForAdmin",
            summary = "Get payment reconciliation case details",
            description = "Requires the ADMIN role and payment:read permission.")
    @GetMapping("/{reconciliationCaseId}")
    public ResponseEntity<ApiResponse<PaymentReconciliationCaseResponse>>
            getPaymentReconciliationCaseForAdmin(
                    @PathVariable("reconciliationCaseId")
                            @UuidV7
                            UUID reconciliationCaseId) {

        PaymentReconciliationCaseResponse response =
                reconciliationManagementReadService.findById(reconciliationCaseId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }
}
