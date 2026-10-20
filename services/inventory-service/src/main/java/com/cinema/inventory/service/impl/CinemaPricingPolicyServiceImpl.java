package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.service.CinemaPricingPolicyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CinemaPricingPolicyServiceImpl implements CinemaPricingPolicyService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyMapper policyMapper;

    public CinemaPricingPolicyServiceImpl(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyMapper policyMapper) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.policyMapper = policyMapper;
    }

    @Override
    @Transactional
    public CinemaPricingPolicyResponse createDraft(
            UUID cinemaId, CreateCinemaPricingPolicyRequest request) {

        Cinema cinema =
                cinemaRepository
                        .findByIdForUpdate(cinemaId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        long maximumVersion = policyRepository.findMaximumPolicyVersionByCinemaId(cinemaId);

        if (maximumVersion == Long.MAX_VALUE) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_EXHAUSTED);
        }

        CinemaPricingPolicy policy =
                new CinemaPricingPolicy(
                        cinema,
                        maximumVersion + 1,
                        request.currency(),
                        request.effectiveFrom(),
                        request.effectiveTo());

        CinemaPricingPolicy savedPolicy = policyRepository.saveAndFlush(policy);

        return policyMapper.toResponse(savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaPricingPolicyResponse> getByCinemaId(UUID cinemaId) {
        requireCinemaExists(cinemaId);

        return policyMapper.toResponses(
                policyRepository.findAllByCinema_IdOrderByPolicyVersionDesc(cinemaId));
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaPricingPolicyResponse getById(UUID cinemaId, UUID policyId) {

        requireCinemaExists(cinemaId);

        CinemaPricingPolicy policy =
                policyRepository
                        .findByIdAndCinema_Id(policyId, cinemaId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        return policyMapper.toResponse(policy);
    }

    private void requireCinemaExists(UUID cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }
    }
}
