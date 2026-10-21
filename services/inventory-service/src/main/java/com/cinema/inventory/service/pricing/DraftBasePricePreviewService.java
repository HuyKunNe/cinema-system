package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.PreviewDraftBasePriceRequest;
import com.cinema.inventory.dto.response.PreviewDraftBasePriceResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DraftBasePricePreviewService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final DraftBasePriceEvaluationService evaluationService;

    public DraftBasePricePreviewService(
            CinemaPricingPolicyRepository policyRepository,
            DraftBasePriceEvaluationService evaluationService) {

        this.policyRepository = policyRepository;
        this.evaluationService = evaluationService;
    }

    @Transactional
    public PreviewDraftBasePriceResponse preview(
            UUID cinemaId, UUID policyId, PreviewDraftBasePriceRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));
        policy.requireBaseRulesModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        BasePriceEvaluationContext context =
                new BasePriceEvaluationContext(
                        request.startsAt(),
                        request.ticketCategory(),
                        request.simulatedMemberTiers(),
                        request.simulatedChannel());

        BasePriceEvaluationResult result = evaluationService.evaluate(cinemaId, policyId, context);

        return PreviewDraftBasePriceResponse.basePriceSimulation(result);
    }
}
