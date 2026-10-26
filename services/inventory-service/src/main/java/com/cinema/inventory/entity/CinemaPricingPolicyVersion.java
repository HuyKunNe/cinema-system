package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
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
@Table(name = "cinema_pricing_policy_versions")
public class CinemaPricingPolicyVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private long revision;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_group", nullable = false, length = 20)
    private PricingDayGroup dayGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PricingPolicyVersionStatus status =
            PricingPolicyVersionStatus.DRAFT;

    protected CinemaPricingPolicyVersion() {}

    public CinemaPricingPolicyVersion(
            CinemaPricingPolicy policy,
            long revision,
            Metadata metadata) {

        if (policy == null
                || policy.getId() == null
                || revision < 1
                || metadata == null) {
            throw invalid();
        }

        this.policy = policy;
        this.revision = revision;
        apply(metadata);
    }

    public void replaceDraftMetadata(Metadata metadata) {
        if (status != PricingPolicyVersionStatus.DRAFT) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_NOT_DRAFT);
        }

        if (metadata == null) {
            throw invalid();
        }

        apply(metadata);
    }

    private void apply(Metadata metadata) {
        this.name = metadata.name();
        this.description = metadata.description();
        this.dayGroup = metadata.dayGroup();
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public long getRevision() {
        return revision;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PricingDayGroup getDayGroup() {
        return dayGroup;
    }

    public PricingPolicyVersionStatus getStatus() {
        return status;
    }

    public record Metadata(
            String name,
            String description,
            PricingDayGroup dayGroup) {

        public Metadata {
            if (name == null || dayGroup == null) {
                throw invalid();
            }

            name = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);

            if (description != null) {
                description =
                        Normalizer.normalize(
                                description.trim(), Normalizer.Form.NFC);

                if (description.isEmpty()) {
                    description = null;
                }
            }

            if (name.isEmpty()
                    || name.length() > 120
                    || (description != null && description.length() > 1000)) {
                throw invalid();
            }
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.INVALID_PRICING_POLICY);
    }
}
