package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.PricingDayGroup;
import com.cinema.inventory.entity.PricingSaleScope;
import com.cinema.inventory.entity.PricingTimeBand;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingDayGroupRepository;
import com.cinema.inventory.repository.PricingSaleScopeRepository;
import com.cinema.inventory.repository.PricingTimeBandRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DraftSaleScopeResolver {

    private final PricingSaleScopeRepository scopeRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;

    public DraftSaleScopeResolver(
            PricingSaleScopeRepository scopeRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository) {

        this.scopeRepository = scopeRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
    }

    // Caller must lock the parent policy and check model, draft and version.
    @Transactional(propagation = Propagation.MANDATORY)
    public SaleScopePlan resolve(FareRuleEvaluationContext context) {
        if (context == null) {
            throw new ValidationException(InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        List<PricingSaleScope> scopes =
                scopeRepository.findAllByPolicy_IdOrderByCodeAsc(context.policyId());

        if (scopes.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.PRICING_SALE_SCOPE_NOT_CONFIGURED);
        }

        Map<String, PricingDayGroup> dayGroups =
                dayGroupRepository.findAllByPolicy_IdOrderByCodeAsc(context.policyId()).stream()
                        .collect(Collectors.toMap(PricingDayGroup::getCode, Function.identity()));

        Map<String, PricingTimeBand> timeBands =
                timeBandRepository
                        .findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(context.policyId())
                        .stream()
                        .collect(Collectors.toMap(PricingTimeBand::getCode, Function.identity()));

        List<SaleWindow> windows = new ArrayList<>();

        for (PricingSaleScope scope : scopes) {
            if (!matchesScenario(scope, context)) {
                continue;
            }

            PricingDayGroup dayGroup = dayGroups.get(scope.getDayGroupCode());
            PricingTimeBand timeBand = timeBands.get(scope.getTimeBandCode());

            if (dayGroup == null || timeBand == null) {
                throw new ConflictException(
                        InventoryErrorCode.PRICING_SALE_SCOPE_REFERENCE_INVALID);
            }

            if (dayGroup.matches(context.localDate(), context.holiday())) {
                windows.add(new SaleWindow(timeBand.getFromMinute(), timeBand.getToMinute()));
            }
        }

        return new SaleScopePlan(windows);
    }

    private boolean matchesScenario(PricingSaleScope scope, FareRuleEvaluationContext context) {

        if (scope.getExperienceType() != context.experienceType()
                || scope.getContentFormat() != context.contentFormat()
                || !scope.getTicketCategoryCode().equals(context.ticketCategoryCode())
                || !scope.getSeatTypeCode().equals(context.seatTypeCode())) {
            return false;
        }

        return switch (scope.getAdvanceScreeningCondition()) {
            case ANY -> true;
            case ADVANCE_SCREENING -> context.advanceScreening();
            case REGULAR_SCREENING -> !context.advanceScreening();
        };
    }

    public record SaleWindow(int fromMinute, int toMinute) {}

    public record SaleScopePlan(List<SaleWindow> windows) {

        public SaleScopePlan {
            windows = List.copyOf(windows);
        }

        public boolean allowsMinute(int minute) {
            if (minute < 0 || minute >= 1440) {
                throw new ValidationException(
                        InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
            }

            return windows.stream()
                    .anyMatch(
                            window -> minute >= window.fromMinute() && minute < window.toMinute());
        }
    }
}
