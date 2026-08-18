package com.cinema.user.service;

import com.cinema.user.dto.request.UpdateCurrentUserProfileRequest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    CurrentUserProfileResponse getCurrentProfile(UUID userId);

    CurrentUserProfileResponse updateCurrentProfile(
            UUID userId, UpdateCurrentUserProfileRequest request);
}
