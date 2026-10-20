package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaHolidayDatesRequest;
import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaHolidayDatesMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaHolidayDatesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaHolidayDatesMapper holidayMapper;

    public CinemaHolidayDatesService(
            CinemaPricingPolicyRepository policyRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaHolidayDatesMapper holidayMapper) {

        this.policyRepository = policyRepository;
        this.holidayRepository = holidayRepository;
        this.holidayMapper = holidayMapper;
    }

    @Transactional
    public CinemaHolidayDatesResponse getHolidayDates(
            UUID cinemaId,
            UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        return readHolidayDates(policy);
    }

    @Transactional
    public CinemaHolidayDatesResponse replaceHolidayDates(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaHolidayDatesRequest request) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        Set<LocalDate> dates = new HashSet<>();

        List<CinemaHolidayDate> newHolidayDates =
                request.holidayDates().stream()
                        .map(
                                input -> {
                                    CinemaHolidayDate holiday =
                                            new CinemaHolidayDate(
                                                    policy,
                                                    input.date(),
                                                    input.name());

                                    if (!dates.add(holiday.getHolidayDate())) {
                                        throw new ValidationException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_HOLIDAY_DATA_INVALID);
                                    }

                                    return holiday;
                                })
                        .toList();

        // Validate the complete replacement before deleting stored dates.
        policy.markContentChanged();

        holidayRepository.deleteAllForPolicy(policy.getId());
        holidayRepository.saveAllAndFlush(newHolidayDates);
        policyRepository.flush();

        return readHolidayDates(policy);
    }

    private CinemaHolidayDatesResponse readHolidayDates(
            CinemaPricingPolicy policy) {

        return holidayMapper.toResponse(
                policy,
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(
                        policy.getId()));
    }
}
