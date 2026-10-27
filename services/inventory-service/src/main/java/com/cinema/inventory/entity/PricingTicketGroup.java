package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.text.Normalizer;

@Entity
@Table(name = "pricing_ticket_groups")
public class PricingTicketGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "policy_version_id",
            nullable = false,
            updatable = false)
    private CinemaPricingPolicyVersion policyVersion;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "eligibility_description", length = 1000)
    private String eligibilityDescription;

    @Column(name = "default_group", nullable = false)
    private boolean defaultGroup;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_category", length = 30, updatable = false)
    private TicketCategory legacyCategory;

    protected PricingTicketGroup() {}

    public PricingTicketGroup(
            CinemaPricingPolicyVersion policyVersion,
            String name,
            String eligibilityDescription,
            boolean defaultGroup,
            int sortOrder) {

        this(
                policyVersion,
                name,
                eligibilityDescription,
                defaultGroup,
                sortOrder,
                null);
    }

    private PricingTicketGroup(
            CinemaPricingPolicyVersion policyVersion,
            String name,
            String eligibilityDescription,
            boolean defaultGroup,
            int sortOrder,
            TicketCategory legacyCategory) {

        if (policyVersion == null
                || policyVersion.getId() == null
                || name == null
                || sortOrder < 0) {

            throw invalid();
        }

        policyVersion.requireDraft();

        String normalizedName =
                Normalizer.normalize(name.trim(), Normalizer.Form.NFC);

        String normalizedDescription =
                eligibilityDescription == null
                        ? null
                        : Normalizer.normalize(
                                eligibilityDescription.trim(),
                                Normalizer.Form.NFC);

        if (normalizedDescription != null
                && normalizedDescription.isEmpty()) {

            normalizedDescription = null;
        }

        if (normalizedName.isEmpty()
                || normalizedName.length() > 120
                || (normalizedDescription != null
                        && normalizedDescription.length() > 1000)
                || (defaultGroup && normalizedDescription != null)) {

            throw invalid();
        }

        this.policyVersion = policyVersion;
        this.name = normalizedName;
        this.eligibilityDescription = normalizedDescription;
        this.defaultGroup = defaultGroup;
        this.sortOrder = sortOrder;
        this.legacyCategory = legacyCategory;
    }

    public static PricingTicketGroup fromLegacy(
            CinemaPricingPolicyVersion owner,
            TicketCategory category) {

        if (category == null) {
            throw invalid();
        }

        String name =
                switch (category) {
                    case ADULT -> "Người lớn";
                    case SENIOR_55_PLUS -> "Người cao tuổi từ 55 tuổi";
                    case STUDENT_U22 -> "Học sinh, sinh viên dưới 22 tuổi";
                    case CHILD -> "Trẻ em";
                };

        return new PricingTicketGroup(
                owner,
                name,
                null,
                category == TicketCategory.ADULT,
                category.ordinal(),
                category);
    }

    public PricingTicketGroup copyTo(
            CinemaPricingPolicyVersion owner) {

        return new PricingTicketGroup(
                owner,
                name,
                eligibilityDescription,
                defaultGroup,
                sortOrder,
                legacyCategory);
    }

    public String getName() {
        return name;
    }

    public String getEligibilityDescription() {
        return eligibilityDescription;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public TicketCategory getLegacyCategory() {
        return legacyCategory;
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.INVALID_PRICING_DEFINITION);
    }
}
