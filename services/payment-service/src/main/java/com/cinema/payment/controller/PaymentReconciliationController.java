package com.cinema.payment.controller;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.payment.controller.request.RejectReconciliationRequest;
import com.cinema.payment.controller.request.ResolveReconciliationRequest;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.service.ReconciliationAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments/reconciliation-cases")
@SecurityRequirement(name = "oauth2", scopes = {"payment:reconcile"})
@Tag(name = "Payment reconciliation operations")
public class PaymentReconciliationController {

    private final ReconciliationAdminService reconciliationAdminService;

    private final Clock clock;

    public PaymentReconciliationController(
            ReconciliationAdminService reconciliationAdminService,
            @Qualifier("systemClock") Clock clock) {

        this.reconciliationAdminService = reconciliationAdminService;
        this.clock = clock;
    }

    @Operation(
            operationId = "resolvePaymentReconciliation",
            summary = "Resolve a refund reconciliation case",
            description =
                    "Requires payment:reconcile permission. "
                            + "Records a verified refund outcome. "
                            + "The server generates correlationId when omitted.")
    @PostMapping("/{reconciliationCaseId}/resolve")
    public ResponseEntity<ApiResponse<ReconciliationOperationResult>>
            resolvePaymentReconciliation(
                    @PathVariable("reconciliationCaseId")
                            @UuidV7
                            UUID reconciliationCaseId,
                    @Valid @RequestBody ResolveReconciliationRequest request,
                    Authentication authentication) {

        ReconciliationResolveRequest serviceRequest =
                new ReconciliationResolveRequest(
                        reconciliationCaseId,
                        request.resolution(),
                        FinancialAuditActorType.USER,
                        authentication.getName(),
                        request.reason(),
                        request.providerReference(),
                        request.failureCode(),
                        request.failureMessage(),
                        correlationIdOrGenerate(request.correlationId()),
                        OffsetDateTime.now(clock));

        ReconciliationOperationResult result =
                reconciliationAdminService.resolve(serviceRequest);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(result));
    }

    @Operation(
            operationId = "rejectPaymentReconciliation",
            summary = "Reject a refund reconciliation case",
            description =
                    "Requires payment:reconcile permission. "
                            + "Rejects the case without recording a successful or failed refund. "
                            + "The server generates correlationId when omitted.")
    @PostMapping("/{reconciliationCaseId}/reject")
    public ResponseEntity<ApiResponse<ReconciliationOperationResult>>
            rejectPaymentReconciliation(
                    @PathVariable("reconciliationCaseId")
                            @UuidV7
                            UUID reconciliationCaseId,
                    @Valid @RequestBody RejectReconciliationRequest request,
                    Authentication authentication) {

        ReconciliationRejectRequest serviceRequest =
                new ReconciliationRejectRequest(
                        reconciliationCaseId,
                        FinancialAuditActorType.USER,
                        authentication.getName(),
                        request.reason(),
                        correlationIdOrGenerate(request.correlationId()),
                        OffsetDateTime.now(clock));

        ReconciliationOperationResult result =
                reconciliationAdminService.reject(serviceRequest);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(result));
    }

    private static UUID correlationIdOrGenerate(UUID correlationId) {

        return correlationId == null ? UuidGenerator.next() : correlationId;
    }
}
