package com.cinema.inventory.service;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.response.CinemaFareMatrixResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaFareMatrixMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.PricingDayGroupRepository;
import com.cinema.inventory.repository.PricingTimeBandRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CinemaFareMatrixService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaFareMatrixMapper matrixMapper;

    public CinemaFareMatrixService(
            CinemaPricingPolicyRepository policyRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository,
            FareRuleRepository fareRuleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaFareMatrixMapper matrixMapper) {

        this.policyRepository = policyRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.holidayRepository = holidayRepository;
        this.matrixMapper = matrixMapper;
    }

    @Transactional
    public CinemaFareMatrixResponse getMatrix(UUID cinemaId, UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();

        UUID scopedPolicyId = policy.getId();

        return matrixMapper.toResponse(
                policy,
                dayGroupRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId),
                timeBandRepository.findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(
                        scopedPolicyId),
                fareRuleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                        scopedPolicyId),
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(
                        scopedPolicyId));
    }
}
