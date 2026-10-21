package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.FareSurchargeRuleRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DraftFixedFareGuard {

    private final FareSurchargeRuleRepository surchargeRepository;

    public DraftFixedFareGuard(FareSurchargeRuleRepository surchargeRepository) {
        this.surchargeRepository = surchargeRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireFixedFares(CinemaPricingPolicy policy) {
        policy.requireFareMatrixModel();

        boolean hasSurcharges =
                !surchargeRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAsc(policy.getId())
                        .isEmpty();

        if (policy.getFareSurchargeMode() != FareSurchargeMode.NONE
                || hasSurcharges) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_FIXED_FARE_REQUIRED);
        }
    }
}
