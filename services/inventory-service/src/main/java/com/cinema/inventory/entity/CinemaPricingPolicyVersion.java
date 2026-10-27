package com.cinema.inventory.entity;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cinema_pricing_policy_versions")
public class CinemaPricingPolicyVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private long revision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_source_version_id", updatable = false)
    private CinemaPricingPolicyVersion draftSourceVersion;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "draft_client_request_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID draftClientRequestId;

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

    @Column(name = "cinema_scope_revision", nullable = false)
    private long cinemaScopeRevision = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pricing_policy_version_cinemas",
            joinColumns = @JoinColumn(name = "policy_version_id"),
            inverseJoinColumns = @JoinColumn(name = "cinema_id"))
    private Set<Cinema> cinemas = new HashSet<>();

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by", length = 255)
    private String publishedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_fare_scopes",
            joinColumns = @JoinColumn(name = "policy_version_id"))
    private Set<FareMatrixScope> fareScopes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_fare_seat_types",
            joinColumns = @JoinColumn(name = "policy_version_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private Set<SeatType> fareSeatTypes = new HashSet<>();

    @OneToMany(mappedBy = "policyVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PricingTimeBandDefinition> pricingTimeBands = new HashSet<>();

    @OneToMany(mappedBy = "policyVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PricingTicketGroup> pricingTicketGroups = new HashSet<>();

    protected CinemaPricingPolicyVersion() {}

    public CinemaPricingPolicyVersion(
            CinemaPricingPolicy policy, long revision, Metadata metadata, Set<Cinema> cinemas) {

        if (policy == null || policy.getId() == null || revision < 1 || metadata == null) {

            throw invalid();
        }

        validateCinemas(cinemas);

        this.policy = policy;
        this.revision = revision;
        this.cinemas.addAll(cinemas);

        apply(metadata);
        initializePricingDefinitions();
    }

    public DraftCopy newDraft(long nextRevision, UUID clientRequestId) {
        if (status != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_SOURCE_NOT_PUBLISHED);
        }

        if (nextRevision <= revision || clientRequestId == null) {
            throw invalid();
        }

        CinemaPricingPolicyVersion draft =
                new CinemaPricingPolicyVersion(
                        policy,
                        nextRevision,
                        new Metadata(name, description, dayGroup),
                        new HashSet<>(cinemas));

        draft.draftSourceVersion = this;
        draft.draftClientRequestId = clientRequestId;

        Set<FareMatrixScope> copiedScopes =
                fareScopes.stream()
                        .map(
                                scope ->
                                        new FareMatrixScope(
                                                scope.getExperienceType(),
                                                scope.getContentFormat()))
                        .collect(Collectors.toSet());

        draft.replaceDraftMatrixDefinition(copiedScopes, new HashSet<>(fareSeatTypes));

        DraftCopy result = draft.copyPricingDefinitionsFrom(this);
        draft.validatePricingDefinitions();

        return result;
    }

    public UUID getDraftSourceVersionId() {
        return draftSourceVersion == null ? null : draftSourceVersion.getId();
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

    public void replaceDraftMatrixDefinition(Set<FareMatrixScope> scopes, Set<SeatType> seatTypes) {

        requireDraft();

        if (scopes == null
                || seatTypes == null
                || scopes.stream().anyMatch(Objects::isNull)
                || seatTypes.stream().anyMatch(Objects::isNull)) {

            throw new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
        }

        fareScopes.clear();
        fareScopes.addAll(scopes);

        fareSeatTypes.clear();
        fareSeatTypes.addAll(seatTypes);

        fareMatrixRevision++;
    }

    public Set<UUID> getCinemaIds() {
        return cinemas.stream().map(Cinema::getId).collect(Collectors.toUnmodifiableSet());
    }

    public boolean appliesTo(UUID cinemaId) {
        return getCinemaIds().contains(cinemaId);
    }

    public void replaceDraftCinemas(Set<Cinema> requestedCinemas) {
        requireDraft();
        validateCinemas(requestedCinemas);

        Set<UUID> requestedIds =
                requestedCinemas.stream().map(Cinema::getId).collect(Collectors.toSet());

        if (getCinemaIds().equals(requestedIds)) {
            return;
        }

        cinemas.clear();
        cinemas.addAll(requestedCinemas);
        cinemaScopeRevision++;
    }

    private static void validateCinemas(Set<Cinema> values) {
        if (values == null
                || values.isEmpty()
                || values.stream().anyMatch(cinema -> cinema == null || cinema.getId() == null)) {

            throw new ValidationException(InventoryErrorCode.PRICING_CINEMA_SCOPE_INVALID);
        }

        long distinctIds = values.stream().map(Cinema::getId).distinct().count();

        if (distinctIds != values.size()) {
            throw new ValidationException(InventoryErrorCode.PRICING_CINEMA_SCOPE_INVALID);
        }
    }

    public Set<FareMatrixScope> getFareScopes() {
        return Set.copyOf(fareScopes);
    }

    public Set<SeatType> getFareSeatTypes() {
        return Set.copyOf(fareSeatTypes);
    }

    private void apply(Metadata metadata) {
        this.name = metadata.name();
        this.description = metadata.description();
        this.dayGroup = metadata.dayGroup();
    }

    public List<PricingTimeBandDefinition> getPricingTimeBands() {
        return pricingTimeBands.stream()
                .sorted(
                        Comparator.comparingInt(PricingTimeBandDefinition::getSortOrder)
                                .thenComparing(PricingTimeBandDefinition::getId))
                .toList();
    }

    public List<PricingTicketGroup> getPricingTicketGroups() {
        return pricingTicketGroups.stream()
                .sorted(
                        Comparator.comparingInt(PricingTicketGroup::getSortOrder)
                                .thenComparing(PricingTicketGroup::getId))
                .toList();
    }

    public void validatePricingDefinitions() {
        if (pricingTimeBands.isEmpty()
                || pricingTicketGroups.isEmpty()
                || pricingTicketGroups.stream().filter(PricingTicketGroup::isDefaultGroup).count()
                        != 1) {

            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
        }

        boolean[] occupied = new boolean[1440];

        for (PricingTimeBandDefinition definition : pricingTimeBands) {
            for (int minute = 0; minute < 1440; minute++) {
                if (!definition.containsMinute(minute)) {
                    continue;
                }

                if (occupied[minute]) {
                    throw new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
                }

                occupied[minute] = true;
            }
        }

        for (PricingTicketGroup group : pricingTicketGroups) {
            if (group.isDefaultGroup() && group.getEligibilityDescription() != null) {

                throw new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
            }
        }
    }

    private void initializePricingDefinitions() {
        pricingTimeBands.add(PricingTimeBandDefinition.fromLegacy(this, PricingTimeBand.BEFORE_17));

        pricingTimeBands.add(PricingTimeBandDefinition.fromLegacy(this, PricingTimeBand.FROM_17));

        pricingTicketGroups.add(PricingTicketGroup.fromLegacy(this, TicketCategory.ADULT));
    }

    private DraftCopy copyPricingDefinitionsFrom(CinemaPricingPolicyVersion source) {

        Map<UUID, UUID> timeBandIds = new HashMap<>();
        Map<UUID, UUID> ticketGroupIds = new HashMap<>();

        pricingTimeBands.clear();

        for (PricingTimeBandDefinition definition : source.pricingTimeBands) {
            PricingTimeBandDefinition copied = definition.copyTo(this);

            pricingTimeBands.add(copied);
            timeBandIds.put(definition.getId(), copied.getId());
        }

        pricingTicketGroups.clear();

        for (PricingTicketGroup group : source.pricingTicketGroups) {
            PricingTicketGroup copied = group.copyTo(this);

            pricingTicketGroups.add(copied);
            ticketGroupIds.put(group.getId(), copied.getId());
        }

        return new DraftCopy(this, Map.copyOf(timeBandIds), Map.copyOf(ticketGroupIds));
    }

    public record DraftCopy(
            CinemaPricingPolicyVersion draft,
            Map<UUID, UUID> timeBandIds,
            Map<UUID, UUID> ticketGroupIds) {}

    private static ConflictException inconsistentDefinitions() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
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

    public void publish(OffsetDateTime publishedAt, String publishedBy) {
        if (status == PricingPolicyVersionStatus.PUBLISHED) {
            return;
        }

        requireDraft();

        if (publishedAt == null
                || publishedBy == null
                || publishedBy.isBlank()
                || publishedBy.length() > 255) {
            throw invalid();
        }
        validatePricingDefinitions();
        this.status = PricingPolicyVersionStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
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
