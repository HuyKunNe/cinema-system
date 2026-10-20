package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaSurchargesRequest;
import com.cinema.inventory.dto.response.CinemaSurchargesResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketSurchargeRule;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaSurchargesMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.TicketSurchargeRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaSurchargesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketSurchargeRuleRepository surchargeRepository;
    private final CinemaSurchargesMapper surchargeMapper;

    public CinemaSurchargesService(
            CinemaPricingPolicyRepository policyRepository,
            TicketSurchargeRuleRepository surchargeRepository,
            CinemaSurchargesMapper surchargeMapper) {

        this.policyRepository = policyRepository;
        this.surchargeRepository = surchargeRepository;
        this.surchargeMapper = surchargeMapper;
    }

    @Transactional
    public CinemaSurchargesResponse getSurcharges(
            UUID cinemaId,
            UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        return readSurcharges(policy);
    }

    @Transactional
    public CinemaSurchargesResponse replaceSurcharges(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaSurchargesRequest request) {

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

        Set<String> codes = new HashSet<>();

        List<TicketSurchargeRule> newSurcharges =
                request.surcharges().stream()
                        .map(
                                input -> {
                                    TicketSurchargeRule rule =
                                            new TicketSurchargeRule(
                                                    policy,
                                                    input.code(),
                                                    input.target(),
                                                    input.targetCode(),
                                                    input.amount(),
                                                    input.unit(),
                                                    input.stackable(),
                                                    input.priority());

                                    if (!codes.add(rule.getCode())) {
                                        throw new ValidationException(
                                                InventoryErrorCode
                                                        .TICKET_SURCHARGE_RULE_DATA_INVALID);
                                    }

                                    return rule;
                                })
                        .toList();

        // Validate the complete replacement before deleting stored rules.
        policy.markContentChanged();

        surchargeRepository.deleteAllForPolicy(policy.getId());
        surchargeRepository.saveAllAndFlush(newSurcharges);
        policyRepository.flush();

        return readSurcharges(policy);
    }

    private CinemaSurchargesResponse readSurcharges(
            CinemaPricingPolicy policy) {

        return surchargeMapper.toResponse(
                policy,
                surchargeRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                                policy.getId()));
    }
}
