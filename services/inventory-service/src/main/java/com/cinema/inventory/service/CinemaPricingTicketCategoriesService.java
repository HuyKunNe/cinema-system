package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaPricingTicketCategoriesRequest;
import com.cinema.inventory.dto.response.CinemaPricingTicketCategoriesResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingTicketCategoriesMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaPricingTicketCategoriesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingTicketCategoryRepository categoryRepository;
    private final CinemaPricingTicketCategoriesMapper categoryMapper;
    private final EntityManager entityManager;

    public CinemaPricingTicketCategoriesService(
            CinemaPricingPolicyRepository policyRepository,
            PricingTicketCategoryRepository categoryRepository,
            CinemaPricingTicketCategoriesMapper categoryMapper,
            EntityManager entityManager) {

        this.policyRepository = policyRepository;
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CinemaPricingTicketCategoriesResponse getCategories(
            UUID cinemaId,
            UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();

        return readCategories(policy);
    }

    @Transactional
    public CinemaPricingTicketCategoriesResponse replaceCategories(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaPricingTicketCategoriesRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        Set<String> codes = new HashSet<>();

        List<PricingTicketCategory> newCategories =
                request.ticketCategories().stream()
                        .map(
                                input -> {
                                    PricingTicketCategory category =
                                            new PricingTicketCategory(
                                                    policy,
                                                    input.code(),
                                                    input.name(),
                                                    input.eligibilityMode(),
                                                    input.eligibilityRuleCode());

                                    if (!codes.add(category.getCode())) {
                                        throw new ValidationException(
                                                InventoryErrorCode
                                                        .PRICING_TICKET_CATEGORY_DATA_INVALID);
                                    }

                                    return category;
                                })
                        .toList();

        policy.markContentChanged();

        categoryRepository.deleteAllForPolicy(policy.getId());
        newCategories.forEach(entityManager::persist);
        entityManager.flush();

        return readCategories(policy);
    }

    private CinemaPricingTicketCategoriesResponse readCategories(
            CinemaPricingPolicy policy) {

        return categoryMapper.toResponse(
                policy,
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(
                        policy.getId()));
    }
}
