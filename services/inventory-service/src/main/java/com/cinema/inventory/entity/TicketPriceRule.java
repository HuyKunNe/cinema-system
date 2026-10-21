package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(
        name = "ticket_price_rules",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ticket_price_rules_policy_code_category",
                    columnNames = {"policy_id", "code", "ticket_category"})
        })
public class TicketPriceRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_category", nullable = false, length = 32)
    private TicketCategory ticketCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private PricingDayType dayType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ticket_price_rule_days", joinColumns = @JoinColumn(name = "rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(name = "from_minute", nullable = false)
    private int fromMinute;

    @Column(name = "to_minute", nullable = false)
    private int toMinute;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "required_member_tier", length = 64)
    private String requiredMemberTier;

    @Column(name = "required_channel", length = 64)
    private String requiredChannel;

    protected TicketPriceRule() {}

    public TicketPriceRule(
            CinemaPricingPolicy policy,
            String code,
            TicketCategory ticketCategory,
            PricingDayType dayType,
            Set<DayOfWeek> daysOfWeek,
            int fromMinute,
            int toMinute,
            BigDecimal amount,
            int priority,
            String requiredMemberTier,
            String requiredChannel) {

        if (policy == null
                || ticketCategory == null
                || dayType == null
                || daysOfWeek == null
                || daysOfWeek.isEmpty()
                || daysOfWeek.stream().anyMatch(day -> day == null)
                || fromMinute < 0
                || fromMinute >= 1440
                || toMinute <= fromMinute
                || toMinute > 1440
                || priority < 0) {
            throw invalid();
        }

        policy.requireBaseRulesModel();
        policy.requireDraft();

        this.policy = policy;
        this.code = normalizeCode(code);
        this.ticketCategory = ticketCategory;
        this.dayType = dayType;
        this.daysOfWeek.addAll(daysOfWeek);
        this.fromMinute = fromMinute;
        this.toMinute = toMinute;
        this.amount = normalizeAmount(amount);
        this.priority = priority;
        this.requiredMemberTier = normalizeOptionalCode(requiredMemberTier);
        this.requiredChannel = normalizeOptionalCode(requiredChannel);
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public String getCode() {
        return code;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    public PricingDayType getDayType() {
        return dayType;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return Set.copyOf(daysOfWeek);
    }

    public int getFromMinute() {
        return fromMinute;
    }

    public int getToMinute() {
        return toMinute;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPriority() {
        return priority;
    }

    public String getRequiredMemberTier() {
        return requiredMemberTier;
    }

    public String getRequiredChannel() {
        return requiredChannel;
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

    private static String normalizeOptionalCode(String value) {
        return value == null ? null : normalizeCode(value);
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
        return new ValidationException(InventoryErrorCode.TICKET_PRICE_RULE_DATA_INVALID);
    }
}
