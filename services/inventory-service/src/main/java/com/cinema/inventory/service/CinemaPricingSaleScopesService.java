package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaPricingSaleScopesRequest;
import com.cinema.inventory.dto.response.CinemaPricingSaleScopesResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSaleScope;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingSaleScopesMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.PricingSaleScopeRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaPricingSaleScopesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingSaleScopeRepository scopeRepository;
    private final CinemaPricingSaleScopesMapper mapper;
    private final EntityManager entityManager;

    public CinemaPricingSaleScopesService(
            CinemaPricingPolicyRepository policyRepository,
            PricingSaleScopeRepository scopeRepository,
            CinemaPricingSaleScopesMapper mapper,
            EntityManager entityManager) {

        this.policyRepository = policyRepository;
        this.scopeRepository = scopeRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CinemaPricingSaleScopesResponse getScopes(UUID cinemaId, UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        return read(policy);
    }

    @Transactional
    public CinemaPricingSaleScopesResponse replaceScopes(
            UUID cinemaId, UUID policyId, ReplaceCinemaPricingSaleScopesRequest request) {

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
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        Set<String> codes = new HashSet<>();
        Set<List<String>> dimensions = new HashSet<>();
        List<PricingSaleScope> prepared = new ArrayList<>();

        for (var input : request.saleScopes()) {
            PricingSaleScope scope =
                    new PricingSaleScope(
                            policy,
                            input.code(),
                            input.experienceType(),
                            input.contentFormat(),
                            input.dayGroupCode(),
                            input.timeBandCode(),
                            input.ticketCategoryCode(),
                            input.seatTypeCode(),
                            input.advanceScreeningCondition());

            List<String> key =
                    List.of(
                            scope.getExperienceType().name(),
                            scope.getContentFormat().name(),
                            scope.getDayGroupCode(),
                            scope.getTimeBandCode(),
                            scope.getTicketCategoryCode(),
                            scope.getSeatTypeCode(),
                            scope.getAdvanceScreeningCondition().name());

            if (!codes.add(scope.getCode()) || !dimensions.add(key)) {
                throw new ValidationException(InventoryErrorCode.PRICING_SALE_SCOPE_DATA_INVALID);
            }

            prepared.add(scope);
        }

        policy.markContentChanged();
        scopeRepository.deleteAllForPolicy(policy.getId());
        prepared.forEach(entityManager::persist);
        entityManager.flush();

        return read(policy);
    }

    private CinemaPricingSaleScopesResponse read(CinemaPricingPolicy policy) {
        return mapper.toResponse(
                policy, scopeRepository.findAllByPolicy_IdOrderByCodeAsc(policy.getId()));
    }
}
