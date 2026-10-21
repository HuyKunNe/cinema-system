package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;

@Entity
@Table(
        name = "pricing_ticket_categories",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_ticket_categories_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class PricingTicketCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_mode", nullable = false, length = 30)
    private TicketEligibilityMode eligibilityMode;

    @Column(name = "eligibility_rule_code", length = 64)
    private String eligibilityRuleCode;

    protected PricingTicketCategory() {}

    public PricingTicketCategory(
            CinemaPricingPolicy policy,
            String code,
            String name,
            TicketEligibilityMode eligibilityMode,
            String eligibilityRuleCode) {

        if (policy == null
                || policy.getId() == null
                || eligibilityMode == null) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        String normalizedRuleCode =
                eligibilityRuleCode == null
                        ? null
                        : normalizeCode(eligibilityRuleCode);

        boolean requiresRule =
                eligibilityMode == TicketEligibilityMode.RULE_REQUIRED;

        if (requiresRule != (normalizedRuleCode != null)) {
            throw invalid();
        }

        this.policy = policy;
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.eligibilityMode = eligibilityMode;
        this.eligibilityRuleCode = normalizedRuleCode;
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public TicketEligibilityMode getEligibilityMode() {
        return eligibilityMode;
    }

    public String getEligibilityRuleCode() {
        return eligibilityRuleCode;
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw invalid();
        }

        return normalized;
    }

    private static String normalizeName(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim();

        if (normalized.isEmpty() || normalized.length() > 150) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.PRICING_TICKET_CATEGORY_DATA_INVALID);
    }
}
