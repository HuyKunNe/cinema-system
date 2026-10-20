package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaPricingPolicyContentRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyContentMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.TicketPriceRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaPricingPolicyContentService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketPriceRuleRepository ruleRepository;
    private final CinemaPricingPolicyContentMapper contentMapper;

    public CinemaPricingPolicyContentService(
            CinemaPricingPolicyRepository policyRepository,
            TicketPriceRuleRepository ruleRepository,
            CinemaPricingPolicyContentMapper contentMapper) {

        this.policyRepository = policyRepository;
        this.ruleRepository = ruleRepository;
        this.contentMapper = contentMapper;
    }

    @Transactional
    public CinemaPricingPolicyContentResponse getContent(
            UUID cinemaId,
            UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        return readContent(policy);
    }

    @Transactional
    public CinemaPricingPolicyContentResponse replaceContent(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaPricingPolicyContentRequest request) {

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

        Set<RuleKey> keys = new HashSet<>();

        List<TicketPriceRule> newRules =
                request.rules().stream()
                        .map(
                                input -> {
                                    TicketPriceRule rule =
                                            new TicketPriceRule(
                                                    policy,
                                                    input.code(),
                                                    input.ticketCategory(),
                                                    input.dayType(),
                                                    input.daysOfWeek(),
                                                    input.fromMinute(),
                                                    input.toMinute(),
                                                    input.amount(),
                                                    input.priority(),
                                                    input.requiredMemberTier(),
                                                    input.requiredChannel());

                                    RuleKey key =
                                            new RuleKey(
                                                    rule.getCode(),
                                                    rule.getTicketCategory());

                                    if (!keys.add(key)) {
                                        throw new ValidationException(
                                                InventoryErrorCode
                                                        .TICKET_PRICE_RULE_DATA_INVALID);
                                    }

                                    return rule;
                                })
                        .toList();

        // Validate every new rule before replacing stored content.
        policy.markContentChanged();

        ruleRepository.deleteAllForPolicy(policy.getId());
        ruleRepository.saveAllAndFlush(newRules);
        policyRepository.flush();

        return readContent(policy);
    }

    private CinemaPricingPolicyContentResponse readContent(
            CinemaPricingPolicy policy) {

        List<TicketPriceRule> rules =
                ruleRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                                policy.getId());

        return contentMapper.toResponse(policy, rules);
    }

    private record RuleKey(String code, TicketCategory ticketCategory) {}
}
