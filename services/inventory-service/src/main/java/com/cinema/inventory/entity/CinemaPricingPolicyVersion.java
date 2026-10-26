package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
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

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
    private PricingPolicyVersionStatus status = PricingPolicyVersionStatus.DRAFT;

    @Column(name = "fare_matrix_revision", nullable = false)
    private long fareMatrixRevision = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_fare_scopes",
            joinColumns = @JoinColumn(name = "policy_version_id"))
    private Set<FareMatrixScope> fareScopes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_fare_categories",
            joinColumns = @JoinColumn(name = "policy_version_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_category", nullable = false, length = 30)
    private Set<TicketCategory> fareTicketCategories = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_fare_seat_types",
            joinColumns = @JoinColumn(name = "policy_version_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private Set<SeatType> fareSeatTypes = new HashSet<>();

    protected CinemaPricingPolicyVersion() {}

    public CinemaPricingPolicyVersion(
            CinemaPricingPolicy policy, long revision, Metadata metadata) {

        if (policy == null || policy.getId() == null || revision < 1 || metadata == null) {
            throw invalid();
        }

        this.policy = policy;
        this.revision = revision;
        apply(metadata);
    }

    public void replaceDraftMetadata(Metadata metadata) {
        requireDraft();

        if (metadata == null) {
            throw invalid();
        }

        if (dayGroup != metadata.dayGroup() && !fareScopes.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.PRICING_MATRIX_DAY_GROUP_LOCKED);
        }

        apply(metadata);
    }

    public void requireDraft() {
        if (status != PricingPolicyVersionStatus.DRAFT) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_DRAFT);
        }
    }

    public void replaceDraftMatrixDefinition(
            Set<FareMatrixScope> scopes, Set<TicketCategory> categories, Set<SeatType> seatTypes) {

        requireDraft();

        if (scopes == null
                || categories == null
                || seatTypes == null
                || scopes.stream().anyMatch(Objects::isNull)
                || categories.stream().anyMatch(Objects::isNull)
                || seatTypes.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
        }

        fareScopes.clear();
        fareScopes.addAll(scopes);

        fareTicketCategories.clear();
        fareTicketCategories.addAll(categories);

        fareSeatTypes.clear();
        fareSeatTypes.addAll(seatTypes);

        fareMatrixRevision++;
    }

    public Set<FareMatrixScope> getFareScopes() {
        return Set.copyOf(fareScopes);
    }

    public Set<TicketCategory> getFareTicketCategories() {
        return Set.copyOf(fareTicketCategories);
    }

    public Set<SeatType> getFareSeatTypes() {
        return Set.copyOf(fareSeatTypes);
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

    public record Metadata(String name, String description, PricingDayGroup dayGroup) {

        public Metadata {
            if (name == null || dayGroup == null) {
                throw invalid();
            }

            name = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);

            if (description != null) {
                description = Normalizer.normalize(description.trim(), Normalizer.Form.NFC);

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
        return new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
    }
}
