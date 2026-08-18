package com.cinema.user.service;

import java.util.UUID;

import com.cinema.user.dto.response.CurrentUserProfileResponse;

public interface UserProfileService {

    CurrentUserProfileResponse getCurrentProfile(UUID userId);
}
