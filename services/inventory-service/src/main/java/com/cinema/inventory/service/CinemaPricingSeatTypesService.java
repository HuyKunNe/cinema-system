package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaPricingSeatTypesRequest;
import com.cinema.inventory.dto.response.CinemaPricingSeatTypesResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingSeatTypesMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.PricingSeatTypeRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaPricingSeatTypesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingSeatTypeRepository seatTypeRepository;
    private final CinemaPricingSeatTypesMapper seatTypeMapper;
    private final EntityManager entityManager;

    public CinemaPricingSeatTypesService(
            CinemaPricingPolicyRepository policyRepository,
            PricingSeatTypeRepository seatTypeRepository,
            CinemaPricingSeatTypesMapper seatTypeMapper,
            EntityManager entityManager) {

        this.policyRepository = policyRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatTypeMapper = seatTypeMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CinemaPricingSeatTypesResponse getSeatTypes(
            UUID cinemaId,
            UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();

        return readSeatTypes(policy);
    }

    @Transactional
    public CinemaPricingSeatTypesResponse replaceSeatTypes(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaPricingSeatTypesRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        Set<String> codes = new HashSet<>();
        Set<SeatType> physicalTypes = EnumSet.noneOf(SeatType.class);
        List<PricingSeatType> newSeatTypes = new ArrayList<>();

        for (ReplaceCinemaPricingSeatTypesRequest.SeatTypeInput input
                : request.seatTypes()) {

            PricingSeatType seatType =
                    new PricingSeatType(
                            policy,
                            input.code(),
                            input.name(),
                            input.mappingStatus(),
                            input.physicalSeatType());

            if (!codes.add(seatType.getCode())) {
                throw invalidSeatTypeData();
            }

            SeatType physicalType = seatType.getPhysicalSeatType();

            if (physicalType != null && !physicalTypes.add(physicalType)) {
                throw invalidSeatTypeData();
            }

            newSeatTypes.add(seatType);
        }

        policy.markContentChanged();

        seatTypeRepository.deleteAllForPolicy(policy.getId());
        newSeatTypes.forEach(entityManager::persist);
        entityManager.flush();

        return readSeatTypes(policy);
    }

    private CinemaPricingSeatTypesResponse readSeatTypes(
            CinemaPricingPolicy policy) {

        return seatTypeMapper.toResponse(
                policy,
                seatTypeRepository.findAllByPolicy_IdOrderByCodeAsc(
                        policy.getId()));
    }

    private static ValidationException invalidSeatTypeData() {
        return new ValidationException(
                InventoryErrorCode.PRICING_SEAT_TYPE_DATA_INVALID);
    }
}
