package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserProfileService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(
            UserRepository userRepository, UserProfileRepository userProfileRepository) {

        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public CurrentUserProfileResponse getCurrentProfile(UUID userId) {

        if (userId == null) {
            throw new ValidationException(UserErrorCode.USER_ID_REQUIRED);
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        UserProfile profile =
                userProfileRepository
                        .findByUser_Id(userId)
                        .orElseThrow(
                                () -> new NotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));

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
