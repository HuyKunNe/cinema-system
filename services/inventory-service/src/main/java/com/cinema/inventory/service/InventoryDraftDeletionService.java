package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.exception.exception.VersionConflictException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryDraftDeletionService {

    private final RoomLayoutRepository layoutRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository pricingVersionRepository;
    private final ShowtimeRepository showtimeRepository;

    public InventoryDraftDeletionService(
            RoomLayoutRepository layoutRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository pricingVersionRepository,
            ShowtimeRepository showtimeRepository) {
        this.layoutRepository = layoutRepository;
        this.policyRepository = policyRepository;
        this.pricingVersionRepository = pricingVersionRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteRoomLayoutDraft(UUID layoutId, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion < 0) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        var layout = layoutRepository.findByIdForUpdate(layoutId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        layout.requireDraft();

        if (!expectedVersion.equals(layout.getVersion())) {
            throw new VersionConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_VERSION_CONFLICT, layout.getVersion());
        }

        if (showtimeRepository.existsByRoomLayout_Id(layoutId)) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_IN_USE);
        }

        layout.deleteDraft();
        layoutRepository.flush();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletePricingPolicyDraft(
            UUID cinemaId, UUID policyId, UUID versionId, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion < 0) {
            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
        }

        // Use the same policy -> version lock order as metadata/configuration/publication writes.
        policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        var revision = pricingVersionRepository.findForUpdate(policyId, versionId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.PRICING_POLICY_VERSION_NOT_FOUND));

        if (!revision.appliesTo(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_FOUND);
        }

        revision.requireDraft();

        if (!expectedVersion.equals(revision.getVersion())) {
            throw new VersionConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT, revision.getVersion());
        }

        if (showtimeRepository.existsByPricingPolicyVersion_Id(versionId)) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_IN_USE);
        }

        revision.deleteDraft();
        pricingVersionRepository.flush();
    }
}
