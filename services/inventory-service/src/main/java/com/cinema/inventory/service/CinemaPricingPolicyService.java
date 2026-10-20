package com.cinema.inventory.service;

import com.cinema.inventory.dto.request.CreateCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;

import java.util.List;
import java.util.UUID;

public interface CinemaPricingPolicyService {

    CinemaPricingPolicyResponse createDraft(
            UUID cinemaId, CreateCinemaPricingPolicyRequest request);

    List<CinemaPricingPolicyResponse> getByCinemaId(UUID cinemaId);

    CinemaPricingPolicyResponse getById(UUID cinemaId, UUID policyId);
}
