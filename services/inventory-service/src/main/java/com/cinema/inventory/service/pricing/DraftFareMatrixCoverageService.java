package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CheckDraftFareMatrixCoverageRequest;
import com.cinema.inventory.dto.response.DraftFareMatrixCoverageResponse;
import com.cinema.inventory.dto.response.DraftFareMatrixCoverageResponse.MatrixCoverageSegment;
import com.cinema.inventory.dto.response.PreviewDraftFareMatrixResponse.MatrixPreviewContext;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaFareMatrixMapper;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class DraftFareMatrixCoverageService {

    private static final int MAX_RETURNED_SEGMENTS = 200;

    private final CinemaPricingPolicyRepository policyRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaPricingPolicyMapper policyMapper;
    private final CinemaFareMatrixMapper matrixMapper;
    private final FareRuleSelector ruleSelector;
    private final DraftTicketCategorySimulationGuard categorySimulationGuard;
    private final DraftSeatTypeSimulationGuard seatTypeSimulationGuard;

    public DraftFareMatrixCoverageService(
            CinemaPricingPolicyRepository policyRepository,
            FareRuleRepository fareRuleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaPricingPolicyMapper policyMapper,
            CinemaFareMatrixMapper matrixMapper,
            FareRuleSelector ruleSelector,
            DraftTicketCategorySimulationGuard categorySimulationGuard,
            DraftSeatTypeSimulationGuard seatTypeSimulationGuard) {

        this.policyRepository = policyRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.holidayRepository = holidayRepository;
        this.policyMapper = policyMapper;
        this.matrixMapper = matrixMapper;
        this.ruleSelector = ruleSelector;
        this.categorySimulationGuard = categorySimulationGuard;
        this.seatTypeSimulationGuard = seatTypeSimulationGuard;
    }

    @Transactional
    public DraftFareMatrixCoverageResponse check(
            UUID cinemaId, UUID policyId, CheckDraftFareMatrixCoverageRequest request) {

        var scenario = request.scenario();

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!scenario.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        if (!"VND".equals(policy.getCurrency())) {
            throw new ValidationException(InventoryErrorCode.FARE_RULE_DATA_INVALID);
        }

        OffsetDateTime localStartsAt = toLocalStartsAt(policy, scenario.startsAt());

        LocalDate localDate = localStartsAt.toLocalDate();
        requireEffectiveDate(policy, localDate);

        boolean holiday =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId()).stream()
                        .anyMatch(entry -> entry.getHolidayDate().equals(localDate));

        FareRuleEvaluationContext context =
                new FareRuleEvaluationContext(
                        policy.getId(),
                        localStartsAt,
                        holiday,
                        scenario.experienceType(),
                        scenario.contentFormat(),
                        scenario.ticketCategoryCode(),
                        scenario.seatTypeCode(),
                        scenario.simulatedReleaseDate(),
                        scenario.simulatedEligibilityCodes());

        categorySimulationGuard.requireForSimulation(context);
        PricingSeatType seatType = seatTypeSimulationGuard.requireForSimulation(context);
        List<FareRule> rules =
                fareRuleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policy.getId());
        TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(request.fromMinute());
        boundaries.add(request.toMinute());

        for (FareRule rule : rules) {
            addBoundary(boundaries, rule.getTimeBand().getFromMinute(), request);
            addBoundary(boundaries, rule.getTimeBand().getToMinute(), request);
        }

        List<Integer> points = new ArrayList<>(boundaries);
        List<MatrixCoverageSegment> segments = new ArrayList<>();

        for (int index = 0; index < points.size() - 1; index++) {
            int from = points.get(index);
            int to = points.get(index + 1);

            FareRuleSelector.Selection selection =
                    ruleSelector.selectAtLocalMinute(rules, context, from);

            seatTypeSimulationGuard.requireCompatibleFareUnits(seatType, selection.winners());

            MatrixCoverageSegment next =
                    new MatrixCoverageSegment(
                            from,
                            to,
                            selection.status(),
                            selection.winners().stream()
                                    .map(matrixMapper::toFareRuleResponse)
                                    .toList());

            appendOrMerge(segments, next);
        }

        // Compute flags over the complete analysis before limiting the response.
        boolean hasGaps =
                segments.stream()
                        .anyMatch(segment -> segment.status() == FareRuleSelectionStatus.GAP);

        boolean hasAmbiguities =
                segments.stream()
                        .anyMatch(segment -> segment.status() == FareRuleSelectionStatus.AMBIGUOUS);

        boolean hasUnconfirmedUnits =
                segments.stream()
                        .flatMap(segment -> segment.winningRules().stream())
                        .anyMatch(rule -> rule.fareUnit() == FareUnit.UNCONFIRMED);

        MatrixPreviewContext referenceContext =
                new MatrixPreviewContext(
                        context.localStartsAt(),
                        context.holiday(),
                        context.experienceType(),
                        context.contentFormat(),
                        context.ticketCategoryCode(),
                        context.seatTypeCode(),
                        context.officialReleaseDate(),
                        context.advanceScreening(),
                        context.eligibilityCodes().stream().sorted().toList());

        return new DraftFareMatrixCoverageResponse(
                policyMapper.toResponse(policy),
                true,
                false,
                false,
                false,
                false,
                true,
                referenceContext,
                request.fromMinute(),
                request.toMinute(),
                hasGaps,
                hasAmbiguities,
                hasUnconfirmedUnits,
                segments.size() > MAX_RETURNED_SEGMENTS,
                segments.stream().limit(MAX_RETURNED_SEGMENTS).toList());
    }

    private void addBoundary(
            TreeSet<Integer> boundaries, int minute, CheckDraftFareMatrixCoverageRequest request) {

        if (minute > request.fromMinute() && minute < request.toMinute()) {
            boundaries.add(minute);
        }
    }

    private void appendOrMerge(List<MatrixCoverageSegment> segments, MatrixCoverageSegment next) {

        if (!segments.isEmpty()) {
            MatrixCoverageSegment previous = segments.getLast();

            if (previous.toMinute() == next.fromMinute()
                    && previous.status() == next.status()
                    && previous.winningRules().equals(next.winningRules())) {

                segments.set(
                        segments.size() - 1,
                        new MatrixCoverageSegment(
                                previous.fromMinute(),
                                next.toMinute(),
                                next.status(),
                                next.winningRules()));

                return;
            }
        }

        segments.add(next);
    }

    private OffsetDateTime toLocalStartsAt(CinemaPricingPolicy policy, OffsetDateTime startsAt) {

        String timeZone = policy.getTimeZone();

        if (timeZone == null || timeZone.isBlank()) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        try {
            return startsAt.toInstant().atZone(ZoneId.of(timeZone)).toOffsetDateTime();
        } catch (DateTimeException exception) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }
    }

    private void requireEffectiveDate(CinemaPricingPolicy policy, LocalDate localDate) {

        if (localDate.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !localDate.isBefore(policy.getEffectiveTo()))) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }
    }
}
