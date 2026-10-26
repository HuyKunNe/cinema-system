package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
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
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

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
    public CreateResult create(
            UUID cinemaId, CreateCinemaPricingPolicyRequest request) {

        if (request == null || request.clientRequestId() == null) {
            throw invalid();
        }

        Metadata metadata =
                new Metadata(
                        request.name(), request.description(), request.dayGroup());

        String fingerprint = fingerprint(metadata);
        Cinema cinema = lockCinema(cinemaId);

        var existing =
                policyRepository.findByCinema_IdAndClientRequestId(
                        cinemaId, request.clientRequestId());

        if (existing.isPresent()) {
            CinemaPricingPolicy policy = existing.get();

            if (!fingerprint.equals(policy.getCreationFingerprint())) {
                throw new ConflictException(
                        InventoryErrorCode.PRICING_POLICY_CREATE_CONFLICT);
            }

            CinemaPricingPolicyVersion firstRevision =
                    versionRepository.findByPolicy_IdAndRevision(policy.getId(), 1)
                            .orElseThrow(
                                    () -> new ConflictException(
                                            InventoryErrorCode
                                                    .PRICING_POLICY_DATA_INCONSISTENT));

            return new CreateResult(false, mapper.toResponse(firstRevision));
        }

        CinemaPricingPolicy policy =
                policyRepository.saveAndFlush(
                        new CinemaPricingPolicy(
                                cinema, request.clientRequestId(), fingerprint));

        CinemaPricingPolicyVersion revision =
                versionRepository.saveAndFlush(
                        new CinemaPricingPolicyVersion(policy, 1, metadata));

        return new CreateResult(true, mapper.toResponse(revision));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CinemaPricingPolicyVersionResponse> listLatest(UUID cinemaId) {
        requireCinema(cinemaId);
        return mapper.toResponses(versionRepository.findLatestForCinema(cinemaId));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CinemaPricingPolicyVersionResponse> listVersions(
            UUID cinemaId, UUID policyId) {

        requireCinema(cinemaId);
        requirePolicy(cinemaId, policyId);

        return mapper.toResponses(
                versionRepository.findAllByPolicy_IdOrderByRevisionDesc(policyId));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CinemaPricingPolicyVersionResponse get(
            UUID cinemaId, UUID policyId, UUID versionId) {

        requireCinema(cinemaId);
        requirePolicy(cinemaId, policyId);

        return mapper.toResponse(
                versionRepository.findForRead(cinemaId, policyId, versionId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode
                                                .PRICING_POLICY_VERSION_NOT_FOUND)));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CinemaPricingPolicyVersionResponse replaceDraft(
            UUID cinemaId,
            UUID policyId,
            UUID versionId,
            ReplacePricingPolicyDraftRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0) {
            throw invalid();
        }

        Metadata metadata =
                new Metadata(
                        request.name(), request.description(), request.dayGroup());

        lockCinema(cinemaId);
        requirePolicy(cinemaId, policyId);

        CinemaPricingPolicyVersion revision =
                versionRepository.findForUpdate(policyId, versionId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode
                                                .PRICING_POLICY_VERSION_NOT_FOUND));

        if (!request.expectedVersion().equals(revision.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        revision.replaceDraftMetadata(metadata);
        versionRepository.flush();

        return mapper.toResponse(revision);
    }

    private Cinema lockCinema(UUID cinemaId) {
        return cinemaRepository.findByIdForUpdate(cinemaId)
                .orElseThrow(
                        () -> new NotFoundException(
                                InventoryErrorCode.CINEMA_NOT_FOUND));
    }

    private void requireCinema(UUID cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }
    }

    private void requirePolicy(UUID cinemaId, UUID policyId) {
        policyRepository.findByIdAndCinema_Id(policyId, cinemaId)
                .orElseThrow(
                        () -> new NotFoundException(
                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));
    }

    private static String fingerprint(Metadata metadata) {
        String canonical =
                framed(metadata.name())
                        + framed(metadata.description())
                        + framed(metadata.dayGroup().name());

        try {
            byte[] hash =
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String framed(String value) {
        String text = value == null ? "" : value;
        return text.length() + ":" + text;
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.INVALID_PRICING_POLICY);
    }

    public record CreateResult(
            boolean created,
            CinemaPricingPolicyVersionResponse response) {}
}
