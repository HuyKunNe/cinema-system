package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EligibilityRuleRegistry {

    private final Map<String, EligibilityRule> rules;
    private final ManualTicketEligibilityService manualEligibilityService;

    @Autowired
    public EligibilityRuleRegistry(
            List<EligibilityRule> configuredRules,
            ManualTicketEligibilityService manualEligibilityService) {

        Map<String, EligibilityRule> indexed = new HashMap<>();

        for (EligibilityRule rule : configuredRules) {
            String code = rule.code();

            if (code == null
                    || !code.matches("[A-Z][A-Z0-9_]{0,63}")
                    || indexed.putIfAbsent(code, rule) != null) {

                throw new IllegalStateException(
                        "Invalid or duplicate eligibility rule code");
            }
        }

        this.rules = Map.copyOf(indexed);
        this.manualEligibilityService = manualEligibilityService;
    }

    public EligibilityRuleRegistry(
            List<EligibilityRule> configuredRules) {

        this(configuredRules, null);
    }

    public EligibilityRule.Decision verify(
            String code,
            EligibilityRule.Context context) {

        EligibilityRule rule = rules.get(code);
        EligibilityRule.Decision decision;

        if (rule != null) {
            if (!rule.supportsCapacity(context.capacity())) {
                throw unavailable();
            }

            decision = rule.verify(context);
        } else {
            if (manualEligibilityService == null) {
                throw unavailable();
            }

            decision = manualEligibilityService.verify(code, context);
        }

        if (decision == null) {
            throw unavailable();
        }

        if (decision.eligible()
                && (decision.reference() == null
                        || decision.reference().isBlank()
                        || decision.reference().length() > 200
                        || (decision.validUntil() != null
                                && !decision.validUntil()
                                        .isAfter(context.checkedAt())))) {

            throw unavailable();
        }

        return decision;
    }

    private static ConflictException unavailable() {
        return new ConflictException(
                InventoryErrorCode.QUOTE_ELIGIBILITY_UNAVAILABLE);
    }
}
