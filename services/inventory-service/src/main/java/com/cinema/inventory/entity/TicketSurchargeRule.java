package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.SurchargeTarget;
import com.cinema.inventory.enums.SurchargeUnit;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;

@Entity
@Table(
        name = "ticket_surcharge_rules",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ticket_surcharge_rules_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class TicketSurchargeRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "surcharge_target", nullable = false, length = 32)
    private SurchargeTarget target;

    @Column(name = "target_code", nullable = false, length = 64)
    private String targetCode;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_unit", nullable = false, length = 30)
    private SurchargeUnit unit;

    @Column(name = "stackable", nullable = false)
    private boolean stackable;

    @Column(name = "priority", nullable = false)
    private int priority;

    protected TicketSurchargeRule() {}

    public TicketSurchargeRule(
            CinemaPricingPolicy policy,
            String code,
            SurchargeTarget target,
            String targetCode,
            BigDecimal amount,
            SurchargeUnit unit,
            boolean stackable,
            int priority) {

        if (policy == null || target == null || unit == null || priority < 0) {
            throw invalid();
        }

        policy.requireBaseRulesModel();
        policy.requireDraft();

        String normalizedTargetCode = normalizeCode(targetCode);

        if (target == SurchargeTarget.SEAT_TYPE
                && Arrays.stream(SeatType.values())
                        .noneMatch(seatType -> seatType.name().equals(normalizedTargetCode))) {
            throw invalid();
        }

        this.policy = policy;
        this.code = normalizeCode(code);
        this.target = target;
        this.targetCode = normalizedTargetCode;
        this.amount = normalizeAmount(amount);
        this.unit = unit;
        this.stackable = stackable;
        this.priority = priority;
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public String getCode() {
        return code;
    }

    public SurchargeTarget getTarget() {
        return target;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SurchargeUnit getUnit() {
        return unit;
    }

    public boolean isStackable() {
        return stackable;
    }

    public int getPriority() {
        return priority;
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

    private static BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw invalid();
        }

        BigDecimal normalized;

        try {
            normalized = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalid();
        }

        if (normalized.precision() > 12) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.TICKET_SURCHARGE_RULE_DATA_INVALID);
    }
}
