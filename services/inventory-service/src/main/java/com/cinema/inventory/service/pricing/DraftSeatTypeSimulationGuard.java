package com.cinema.inventory.service.pricing;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingSeatTypeRepository;

@Component
public class DraftSeatTypeSimulationGuard {

    private final PricingSeatTypeRepository seatTypeRepository;

    public DraftSeatTypeSimulationGuard(PricingSeatTypeRepository seatTypeRepository) {

        this.seatTypeRepository = seatTypeRepository;
    }

    /**
     * The caller must first lock the parent policy and validate its model, draft status and
     * expected version.
     *
     * <p>Checks stored catalog configuration only. Does not verify an actual seat, room
     * compatibility or charging units.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PricingSeatType requireForSimulation(FareRuleEvaluationContext context) {

        if (context == null) {
            throw new ValidationException(InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        PricingSeatType seatType =
                seatTypeRepository
                        .findByPolicy_IdAndCode(context.policyId(), context.seatTypeCode())
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                InventoryErrorCode
                                                        .PRICING_SEAT_TYPE_NOT_CONFIGURED));

        if (seatType.getMappingStatus() != PricingSeatMappingStatus.CONFIRMED
                || seatType.getPhysicalSeatType() == null) {

            throw new ConflictException(InventoryErrorCode.PRICING_SEAT_TYPE_MAPPING_UNCONFIRMED);
        }

        return seatType;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireCompatibleFareUnits(PricingSeatType seatType, List<FareRule> winningRules) {

        if (seatType == null || winningRules == null) {
            throw new ValidationException(InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        for (FareRule rule : winningRules) {
            if (!seatType.acceptsFareUnit(rule.getFareUnit())) {
                throw new ConflictException(InventoryErrorCode.PRICING_PAIR_FARE_UNIT_REQUIRED);
            }
        }
    }
}
