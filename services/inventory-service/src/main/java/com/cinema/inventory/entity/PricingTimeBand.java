package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;

@Entity
@Table(
        name = "pricing_time_bands",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_time_bands_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class PricingTimeBand extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "from_minute", nullable = false)
    private int fromMinute;

    @Column(name = "to_minute", nullable = false)
    private int toMinute;

    protected PricingTimeBand() {}

    public PricingTimeBand(
            CinemaPricingPolicy policy,
            String code,
            String name,
            int fromMinute,
            int toMinute) {

        if (policy == null
                || fromMinute < 0
                || fromMinute >= 1440
                || toMinute <= fromMinute
                || toMinute > 1440) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        this.policy = policy;
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.fromMinute = fromMinute;
        this.toMinute = toMinute;
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

    public int getFromMinute() {
        return fromMinute;
    }

    public int getToMinute() {
        return toMinute;
    }

    public boolean matches(int localMinute) {
        if (localMinute < 0 || localMinute >= 1440) {
            throw invalid();
        }

        return localMinute >= fromMinute && localMinute < toMinute;
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z0-9][A-Z0-9_]{0,63}")) {
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
                InventoryErrorCode.PRICING_DIMENSION_DATA_INVALID);
    }
}
