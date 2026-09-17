package com.cinema.payment.controller;

import com.cinema.payment.controller.request.RejectReconciliationRequest;
import com.cinema.payment.controller.request.ResolveReconciliationRequest;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.service.ReconciliationAdminService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/reconciliation-cases")
public class PaymentReconciliationController {

    private final ReconciliationAdminService reconciliationAdminService;

    public PaymentReconciliationController(
            ReconciliationAdminService reconciliationAdminService) {

        this.reconciliationAdminService =
                reconciliationAdminService;
    }

    @PostMapping("/{reconciliationCaseId}/resolve")
    public ResponseEntity<ReconciliationOperationResult> resolve(
            @PathVariable UUID reconciliationCaseId,
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
                        request.correlationId(),
                        OffsetDateTime.now(ZoneOffset.UTC));

        ReconciliationOperationResult result =
                reconciliationAdminService.resolve(serviceRequest);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{reconciliationCaseId}/reject")
    public ResponseEntity<ReconciliationOperationResult> reject(
            @PathVariable UUID reconciliationCaseId,
            @Valid @RequestBody RejectReconciliationRequest request,
            Authentication authentication) {

        ReconciliationRejectRequest serviceRequest =
                new ReconciliationRejectRequest(
                        reconciliationCaseId,
                        FinancialAuditActorType.USER,
                        authentication.getName(),
                        request.reason(),
                        request.correlationId(),
                        OffsetDateTime.now(ZoneOffset.UTC));

        ReconciliationOperationResult result =
                reconciliationAdminService.reject(serviceRequest);

        return ResponseEntity.ok(result);
    }
}
