package com.cinema.inventory.service.pricing;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;

@Component
public class DraftTicketCategorySimulationGuard {

    private final PricingTicketCategoryRepository categoryRepository;

    public DraftTicketCategorySimulationGuard(
            PricingTicketCategoryRepository categoryRepository) {

        this.categoryRepository = categoryRepository;
    }

    /**
     * The caller must first lock the parent policy and validate its model,
     * draft status and expected version.
     *
     * This checks simulation assumptions, not actual customer eligibility.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireForSimulation(FareRuleEvaluationContext context) {

        if (context == null) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        PricingTicketCategory category =
                categoryRepository
                        .findByPolicy_IdAndCode(
                                context.policyId(),
                                context.ticketCategoryCode())
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                InventoryErrorCode
                                                        .PRICING_TICKET_CATEGORY_NOT_CONFIGURED));

        switch (category.getEligibilityMode()) {
            case UNCONFIRMED ->
                    throw new ConflictException(
                            InventoryErrorCode
                                    .PRICING_TICKET_CATEGORY_ELIGIBILITY_UNCONFIRMED);

            case UNRESTRICTED -> {
                // Fare-specific conditions are still checked by FareRuleSelector.
            }

            case RULE_REQUIRED -> {
                String requiredCode = category.getEligibilityRuleCode();

                if (requiredCode == null || requiredCode.isBlank()) {
                    throw new ConflictException(
                            InventoryErrorCode
                                    .PRICING_TICKET_CATEGORY_ELIGIBILITY_UNCONFIRMED);
                }

                if (!context.eligibilityCodes().contains(requiredCode)) {
                    throw new ConflictException(
                            InventoryErrorCode
                                    .PRICING_TICKET_CATEGORY_ELIGIBILITY_NOT_SIMULATED);
                }
            }
        }
    }
}
