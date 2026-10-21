package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
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
        name = "pricing_seat_types",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_seat_types_policy_code",
                    columnNames = {"policy_id", "code"}),
            @UniqueConstraint(
                    name = "uk_pricing_seat_types_policy_physical",
                    columnNames = {"policy_id", "physical_seat_type"})
        })
public class PricingSeatType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_status", nullable = false, length = 30)
    private PricingSeatMappingStatus mappingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_seat_type", length = 30)
    private SeatType physicalSeatType;

    protected PricingSeatType() {}

    public PricingSeatType(
            CinemaPricingPolicy policy,
            String code,
            String name,
            PricingSeatMappingStatus mappingStatus,
            SeatType physicalSeatType) {

        if (policy == null
                || policy.getId() == null
                || mappingStatus == null) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        boolean confirmed =
                mappingStatus == PricingSeatMappingStatus.CONFIRMED;

        if (confirmed != (physicalSeatType != null)) {
            throw invalid();
        }

        this.policy = policy;
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.mappingStatus = mappingStatus;
        this.physicalSeatType = physicalSeatType;
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

    public PricingSeatMappingStatus getMappingStatus() {
        return mappingStatus;
    }

    public SeatType getPhysicalSeatType() {
        return physicalSeatType;
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
                InventoryErrorCode.PRICING_SEAT_TYPE_DATA_INVALID);
    }
}
