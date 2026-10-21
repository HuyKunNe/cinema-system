package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingSeatTypeRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DraftSeatTypeSimulationGuard {

    private final PricingSeatTypeRepository seatTypeRepository;

    public DraftSeatTypeSimulationGuard(
            PricingSeatTypeRepository seatTypeRepository) {

        this.seatTypeRepository = seatTypeRepository;
    }

    /**
     * The caller must first lock the parent policy and validate its model,
     * draft status and expected version.
     *
     * Checks stored catalog configuration only.
     * Does not verify an actual seat, room compatibility or charging units.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireForSimulation(FareRuleEvaluationContext context) {

        if (context == null) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        PricingSeatType seatType =
                seatTypeRepository
                        .findByPolicy_IdAndCode(
                                context.policyId(),
                                context.seatTypeCode())
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                InventoryErrorCode
                                                        .PRICING_SEAT_TYPE_NOT_CONFIGURED));

        if (seatType.getMappingStatus()
                        != PricingSeatMappingStatus.CONFIRMED
                || seatType.getPhysicalSeatType() == null) {

            throw new ConflictException(
                    InventoryErrorCode.PRICING_SEAT_TYPE_MAPPING_UNCONFIRMED);
        }
    }
}
