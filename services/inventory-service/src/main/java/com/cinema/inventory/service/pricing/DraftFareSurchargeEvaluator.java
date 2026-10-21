package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.response.DraftFareSurchargeEvaluationResponse;
import com.cinema.inventory.dto.response.DraftFareSurchargeEvaluationResponse.SurchargeEvaluationStatus;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.FareSurchargeRule;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareSurchargeUnit;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaFareSurchargesMapper;
import com.cinema.inventory.repository.FareSurchargeRuleRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class DraftFareSurchargeEvaluator {

    private final FareSurchargeRuleRepository repository;
    private final CinemaFareSurchargesMapper mapper;

    public DraftFareSurchargeEvaluator(
            FareSurchargeRuleRepository repository,
            CinemaFareSurchargesMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Caller must lock the parent policy, check expectedVersion,
     * and validate category, seat mapping and sale scope.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PreparedEvaluation prepare(
            CinemaPricingPolicy policy,
            FareRuleEvaluationContext context,
            PricingSeatType seatType,
            Set<String> simulatedMovieAttributeCodes) {

        if (policy == null
                || context == null
                || seatType == null
                || !policy.getId().equals(context.policyId())
                || !policy.getId().equals(seatType.getPolicy().getId())
                || !seatType.getCode().equals(context.seatTypeCode())) {
            throw invalidContext();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        FareSurchargeMode mode = policy.getFareSurchargeMode();

        if (mode == null || mode == FareSurchargeMode.UNCONFIRMED) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_FARE_SURCHARGES_UNCONFIRMED);
        }

        Set<String> movieCodes = normalizeCodes(simulatedMovieAttributeCodes);

        List<FareSurchargeRule> stored =
                repository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                        policy.getId());

        if (mode == FareSurchargeMode.NONE && !stored.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_FARE_SURCHARGES_INVALID);
        }

        Map<String, List<FareSurchargeRule>> byFare =
                stored.stream()
                        .filter(rule -> matchesConditions(rule, context, movieCodes))
                        .sorted(Comparator.comparing(FareSurchargeRule::getCode))
                        .collect(Collectors.groupingBy(
                                FareSurchargeRule::getFareRuleCode));

        byFare.replaceAll((code, rules) -> List.copyOf(rules));

        return new PreparedEvaluation(
                mode,
                Map.copyOf(byFare),
                context,
                seatType.getPhysicalSeatType() == SeatType.COUPLE,
                movieCodes.stream().sorted().toList());
    }

    private boolean matchesConditions(
            FareSurchargeRule rule,
            FareRuleEvaluationContext context,
            Set<String> movieCodes) {

        if (context.localDate().isBefore(rule.getEffectiveFrom())
                || (rule.getEffectiveTo() != null
                        && !context.localDate().isBefore(rule.getEffectiveTo()))) {
            return false;
        }

        if (rule.getMovieAttributeCode() != null
                && !movieCodes.contains(rule.getMovieAttributeCode())) {
            return false;
        }

        return rule.getRequiredEligibilityCode() == null
                || context.eligibilityCodes()
                        .contains(rule.getRequiredEligibilityCode());
    }

    private Set<String> normalizeCodes(Set<String> input) {
        if (input == null) {
            return Set.of();
        }

        Set<String> result = new TreeSet<>();

        for (String value : input) {
            if (value == null) {
                throw invalidContext();
            }

            String code = value.trim().toUpperCase(Locale.ROOT);

            if (!code.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw invalidContext();
            }

            result.add(code);
        }

        return Set.copyOf(result);
    }

    private static ValidationException invalidContext() {
        return new ValidationException(
                InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
    }

    public final class PreparedEvaluation {

        private final FareSurchargeMode mode;
        private final Map<String, List<FareSurchargeRule>> byFare;
        private final FareRuleEvaluationContext context;
        private final boolean coupleSeat;
        private final List<String> movieCodes;

        private PreparedEvaluation(
                FareSurchargeMode mode,
                Map<String, List<FareSurchargeRule>> byFare,
                FareRuleEvaluationContext context,
                boolean coupleSeat,
                List<String> movieCodes) {

            this.mode = mode;
            this.byFare = byFare;
            this.context = context;
            this.coupleSeat = coupleSeat;
            this.movieCodes = List.copyOf(movieCodes);
        }

        public DraftFareSurchargeEvaluationResponse evaluate(
                FareRuleSelector.Selection selection) {

            if (selection == null) {
                throw invalidContext();
            }

            if (selection.status() != FareRuleSelectionStatus.MATCHED) {
                return result(
                        SurchargeEvaluationStatus.FARE_NOT_RESOLVED,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        null);
            }

            FareRule fare = selection.winners().getFirst();

            if (!fare.getPolicy().getId().equals(context.policyId())
                    || !fare.getSeatTypeCode().equals(context.seatTypeCode())
                    || !fare.getTicketCategoryCode()
                            .equals(context.ticketCategoryCode())) {
                throw invalidContext();
            }

            List<FareSurchargeRule> matched =
                    byFare.getOrDefault(fare.getCode(), List.of());

            List<FareSurchargeRule> selected = matched;

            if (mode == FareSurchargeMode.HIGHEST_PRIORITY && !matched.isEmpty()) {
                int highest =
                        matched.stream()
                                .mapToInt(FareSurchargeRule::getPriority)
                                .max()
                                .orElseThrow();

                selected =
                        matched.stream()
                                .filter(rule -> rule.getPriority() == highest)
                                .toList();

                if (selected.size() > 1) {
                    return result(
                            SurchargeEvaluationStatus.AMBIGUOUS,
                            fare,
                            matched,
                            selected,
                            null,
                            null);
                }
            }

            if (fare.getFareUnit() == FareUnit.UNCONFIRMED
                    || selected.stream().anyMatch(
                            rule -> rule.getUnit() == FareSurchargeUnit.UNCONFIRMED)) {

                return result(
                        SurchargeEvaluationStatus.UNIT_UNCONFIRMED,
                        fare,
                        matched,
                        selected,
                        null,
                        null);
            }

            if (coupleSeat
                    && selected.stream().anyMatch(
                            rule -> rule.getUnit() != FareSurchargeUnit.PER_PAIR)) {

                throw new ConflictException(
                        InventoryErrorCode.PRICING_PAIR_SURCHARGE_UNIT_REQUIRED);
            }

            boolean compatibleUnits =
                    selected.stream().allMatch(rule ->
                            (fare.getFareUnit() == FareUnit.PER_TICKET
                                    && rule.getUnit() == FareSurchargeUnit.PER_TICKET)
                            || (fare.getFareUnit() == FareUnit.PER_PAIR
                                    && rule.getUnit() == FareSurchargeUnit.PER_PAIR));

            if (!compatibleUnits) {
                return result(
                        SurchargeEvaluationStatus.BOOKING_CONTEXT_REQUIRED,
                        fare,
                        matched,
                        selected,
                        null,
                        null);
            }

            try {
                long additional = 0;

                for (FareSurchargeRule rule : selected) {
                    additional = Math.addExact(additional, rule.getAmountVnd());
                }

                long combined = Math.addExact(fare.getAmountVnd(), additional);

                return result(
                        selected.isEmpty()
                                ? SurchargeEvaluationStatus.NONE
                                : SurchargeEvaluationStatus.MATCHED,
                        fare,
                        matched,
                        selected,
                        additional,
                        combined);
            } catch (ArithmeticException exception) {
                throw new ConflictException(
                        InventoryErrorCode.PRICING_UNIT_AMOUNT_OVERFLOW);
            }
        }

        private DraftFareSurchargeEvaluationResponse result(
                SurchargeEvaluationStatus status,
                FareRule fare,
                List<FareSurchargeRule> matched,
                List<FareSurchargeRule> selected,
                Long additional,
                Long combined) {

            return new DraftFareSurchargeEvaluationResponse(
                    mode,
                    status,
                    fare == null ? null : fare.getFareUnit(),
                    fare == null ? null : Long.toString(fare.getAmountVnd()),
                    additional == null ? null : Long.toString(additional),
                    combined == null ? null : Long.toString(combined),
                    matched.stream().map(mapper::toRuleResponse).toList(),
                    selected.stream().map(mapper::toRuleResponse).toList(),
                    false,
                    movieCodes);
        }
    }
}
