package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreateCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.request.ReplacePricingPolicyDraftRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion.Metadata;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CinemaPricingPolicyService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final CinemaPricingPolicyMapper mapper;

    public CinemaPricingPolicyService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            CinemaPricingPolicyMapper mapper) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.mapper = mapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreateResult create(UUID cinemaId, CreateCinemaPricingPolicyRequest request) {

        if (request == null || request.clientRequestId() == null) {
            throw invalid();
        }

        Metadata metadata = new Metadata(request.name(), request.description(), request.dayGroup());

        Set<UUID> cinemaIds = normalizeCinemaIds(request.cinemaIds());

        if (!cinemaIds.contains(cinemaId)) {
            throw invalidScope();
        }

        String metadataFingerprint = fingerprint(metadata);
        String scopeFingerprint = scopeFingerprint(cinemaIds);

        Set<Cinema> cinemas = lockCinemas(cinemaIds);

        Cinema creationCinema =
                cinemas.stream()
                        .filter(cinema -> cinema.getId().equals(cinemaId))
                        .findFirst()
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        var existing =
                policyRepository.findByCreationCinema_IdAndClientRequestId(
                        cinemaId, request.clientRequestId());

        if (existing.isPresent()) {
            CinemaPricingPolicy policy = existing.get();

            if (!metadataFingerprint.equals(policy.getCreationFingerprint())
                    || !scopeFingerprint.equals(policy.getCreationScopeFingerprint())) {

                throw new ConflictException(InventoryErrorCode.PRICING_POLICY_CREATE_CONFLICT);
            }

            CinemaPricingPolicyVersion firstRevision =
                    versionRepository
                            .findByPolicy_IdAndRevision(policy.getId(), 1)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    InventoryErrorCode
                                                            .PRICING_POLICY_DATA_INCONSISTENT));

            return new CreateResult(false, mapper.toResponse(firstRevision));
        }

        CinemaPricingPolicy policy =
                policyRepository.saveAndFlush(
                        new CinemaPricingPolicy(
                                creationCinema,
                                request.clientRequestId(),
                                metadataFingerprint,
                                scopeFingerprint));

        CinemaPricingPolicyVersion revision =
                versionRepository.saveAndFlush(
                        new CinemaPricingPolicyVersion(policy, 1, metadata, cinemas));

        return new CreateResult(true, mapper.toResponse(revision));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CinemaPricingPolicyVersionResponse> listLatest(UUID cinemaId) {

        requireCinema(cinemaId);

        return mapper.toResponses(versionRepository.findLatestForCinema(cinemaId));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CinemaPricingPolicyVersionResponse> listVersions(UUID cinemaId, UUID policyId) {

        requireCinema(cinemaId);

        List<CinemaPricingPolicyVersion> versions =
                versionRepository.findAllForCinema(cinemaId, policyId);

        if (versions.isEmpty()) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND);
        }

        return mapper.toResponses(versions);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CinemaPricingPolicyVersionResponse get(UUID cinemaId, UUID policyId, UUID versionId) {

        requireCinema(cinemaId);
        requirePolicy(policyId);

        CinemaPricingPolicyVersion revision =
                versionRepository
                        .findForRead(cinemaId, policyId, versionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_VERSION_NOT_FOUND));

        return mapper.toResponse(revision);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CinemaPricingPolicyVersionResponse replaceDraft(
            UUID cinemaId,
            UUID policyId,
            UUID versionId,
            ReplacePricingPolicyDraftRequest request) {

        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {

            throw invalid();
        }

        Metadata metadata = new Metadata(request.name(), request.description(), request.dayGroup());

        Set<UUID> cinemaIds = normalizeCinemaIds(request.cinemaIds());

        requireCinema(cinemaId);

        policyRepository
                .findByIdForUpdate(policyId)
                .orElseThrow(
                        () -> new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        CinemaPricingPolicyVersion revision =
                versionRepository
                        .findForUpdate(policyId, versionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_VERSION_NOT_FOUND));

        if (!revision.appliesTo(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_FOUND);
        }

        if (!request.expectedVersion().equals(revision.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        revision.requireDraft();

        Set<Cinema> cinemas = requireCinemas(cinemaIds);

        revision.replaceDraftMetadata(metadata);
        revision.replaceDraftCinemas(cinemas);

        versionRepository.flush();

        return mapper.toResponse(revision);
    }

    private Set<Cinema> lockCinemas(Set<UUID> cinemaIds) {
        Set<Cinema> result = new HashSet<>();

        for (UUID cinemaId : cinemaIds.stream().sorted().toList()) {
            Cinema cinema =
                    cinemaRepository
                            .findByIdForUpdate(cinemaId)
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    InventoryErrorCode.CINEMA_NOT_FOUND));

            result.add(cinema);
        }

        return result;
    }

    private Set<Cinema> requireCinemas(Set<UUID> cinemaIds) {
        List<Cinema> cinemas = cinemaRepository.findAllById(cinemaIds);

        if (cinemas.size() != cinemaIds.size()) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }

        return new HashSet<>(cinemas);
    }

    private Set<UUID> normalizeCinemaIds(List<UUID> values) {
        if (values == null || values.isEmpty()) {
            throw invalidScope();
        }

        Set<UUID> cinemaIds = new HashSet<>();

        for (UUID value : values) {
            if (value == null || !cinemaIds.add(value)) {
                throw invalidScope();
            }
        }

        return Set.copyOf(cinemaIds);
    }

    private void requireCinema(UUID cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }
    }

    private void requirePolicy(UUID policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND);
        }
    }

    private static String fingerprint(Metadata metadata) {
        return hash(
                framed(metadata.name())
                        + framed(metadata.description())
                        + framed(metadata.dayGroup().name()));
    }

    private static String scopeFingerprint(Set<UUID> cinemaIds) {
        String canonical =
                cinemaIds.stream()
                        .map(id -> id.toString().replace("-", ""))
                        .sorted()
                        .map(CinemaPricingPolicyService::framed)
                        .collect(Collectors.joining());

        return hash(canonical);
    }

    private static String hash(String canonical) {
        try {
            byte[] hash =
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.PRICING_FINGERPRINT_UNAVAILABLE, exception);
        }
    }

    private static String framed(String value) {
        String text = value == null ? "" : value;
        return text.length() + ":" + text;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
    }

    private static ValidationException invalidScope() {
        return new ValidationException(InventoryErrorCode.PRICING_CINEMA_SCOPE_INVALID);
    }

    public record CreateResult(boolean created, CinemaPricingPolicyVersionResponse response) {}
}
