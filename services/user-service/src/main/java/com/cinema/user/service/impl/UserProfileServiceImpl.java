package com.cinema.user.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.core.util.StringUtils;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.dto.request.UpdateCurrentUserProfileRequest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserProfileService;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository) {

        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public CurrentUserProfileResponse getCurrentProfile(UUID userId) {

        requireUserId(userId);

        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        return toResponse(user, profile);
    }

    @Override
    @Transactional
    public CurrentUserProfileResponse updateCurrentProfile(
            UUID userId,
            UpdateCurrentUserProfileRequest request) {

        requireUserId(userId);

        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        profile.update(
                StringUtils.trimToNull(request.firstName()),
                StringUtils.trimToNull(request.lastName()),
                StringUtils.trimToNull(request.phoneNumber()));

        return toResponse(user, profile);
    }

    private static void requireUserId(UUID userId) {

        if (userId == null) {
            throw new ValidationException(
                    UserErrorCode.USER_ID_REQUIRED);
        }
    }

    private User findUser(UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new NotFoundException(
                                UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findProfile(UUID userId) {

        return userProfileRepository
                .findByUser_Id(userId)
                .orElseThrow(
                        () -> new NotFoundException(
                                UserErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private static CurrentUserProfileResponse toResponse(
            User user,
            UserProfile profile) {

        return new CurrentUserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getStatus(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
