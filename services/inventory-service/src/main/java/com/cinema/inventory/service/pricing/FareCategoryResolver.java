package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.enums.TicketPricingRole;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FareCategoryResolver {

    private final PricingTicketCategoryRepository categoryRepository;

    public FareCategoryResolver(PricingTicketCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Caller must already hold the parent policy lock.
     * Resolves price classification only; does not verify customer evidence.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Resolution resolve(FareRuleEvaluationContext source) {
        if (source == null) {
            throw invalidContext();
        }

        return resolve(
                source,
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(source.policyId()));
    }

    /**
     * Pure snapshot overload used by full-policy validation.
     */
    public Resolution resolve(
            FareRuleEvaluationContext source,
            List<PricingTicketCategory> categories) {

        if (source == null || categories == null) {
            throw invalidContext();
        }

        for (PricingTicketCategory category : categories) {
            if (category == null
                    || !source.policyId().equals(category.getPolicy().getId())
                    || category.getPricingRole() == null
                    || category.getPricingRole() == TicketPricingRole.UNCONFIRMED) {
                throw invalidConfiguration();
            }
        }

        List<PricingTicketCategory> regularCategories =
                categories.stream()
                        .filter(category ->
                                category.getPricingRole() == TicketPricingRole.REGULAR)
                        .toList();

        if (regularCategories.size() != 1) {
            throw invalidConfiguration();
        }

        PricingTicketCategory regular = regularCategories.getFirst();

        if (regular.getEligibilityMode() != TicketEligibilityMode.UNRESTRICTED) {
            throw invalidConfiguration();
        }

        PricingTicketCategory requested =
                categories.stream()
                        .filter(category ->
                                category.getCode().equals(source.ticketCategoryCode()))
                        .findFirst()
                        .orElseThrow(() ->
                                new ConflictException(
                                        InventoryErrorCode
                                                .PRICING_TICKET_CATEGORY_NOT_CONFIGURED));

        AdjustmentReason reason = AdjustmentReason.NONE;
        PricingTicketCategory effective = requested;

        if (source.advanceScreening()
                && requested.getPricingRole().excludedFromAdvanceScreening()) {
            effective = regular;
            reason = AdjustmentReason.ADVANCE_SCREENING_REGULAR_PRICE;
        } else if (source.experienceType() == ExperienceType.PREMIUM_CINEMA
                && requested.getPricingRole() == TicketPricingRole.CATEGORY_DISCOUNT) {
            effective = regular;
            reason = AdjustmentReason.PREMIUM_CATEGORY_DISCOUNT_DISABLED;
        }

        Set<String> disabledEligibilityCodes =
                categories.stream()
                        .filter(category -> disabled(category.getPricingRole(), source))
                        .map(PricingTicketCategory::getEligibilityRuleCode)
                        .filter(code -> code != null)
                        .collect(Collectors.toUnmodifiableSet());

        // A regular fare must not gain a promotional price from submitted codes.
        Set<String> effectiveEligibilityCodes =
                effective.getPricingRole() == TicketPricingRole.REGULAR
                        ? Set.of()
                        : source.eligibilityCodes().stream()
                                .filter(code -> !disabledEligibilityCodes.contains(code))
                                .collect(Collectors.toUnmodifiableSet());

        FareRuleEvaluationContext resolved =
                new FareRuleEvaluationContext(
                        source.policyId(),
                        source.localStartsAt(),
                        source.holiday(),
                        source.experienceType(),
                        source.contentFormat(),
                        effective.getCode(),
                        source.seatTypeCode(),
                        source.officialReleaseDate(),
                        effectiveEligibilityCodes);

        return new Resolution(source.ticketCategoryCode(), reason, resolved);
    }

    private boolean disabled(
            TicketPricingRole role,
            FareRuleEvaluationContext context) {

        return (context.advanceScreening()
                        && role.excludedFromAdvanceScreening())
                || (context.experienceType() == ExperienceType.PREMIUM_CINEMA
                        && role == TicketPricingRole.CATEGORY_DISCOUNT);
    }

    private static ConflictException invalidConfiguration() {
        return new ConflictException(
                InventoryErrorCode.PRICING_CATEGORY_RULES_INVALID);
    }

    private static ValidationException invalidContext() {
        return new ValidationException(
                InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
    }

    public enum AdjustmentReason {
        NONE,
        ADVANCE_SCREENING_REGULAR_PRICE,
        PREMIUM_CATEGORY_DISCOUNT_DISABLED
    }

    public record Resolution(
            String requestedTicketCategoryCode,
            AdjustmentReason reason,
            FareRuleEvaluationContext context) {}
}
