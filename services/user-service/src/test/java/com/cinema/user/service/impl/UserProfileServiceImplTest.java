package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.dto.request.UpdateCurrentUserProfileRequest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("019c9000-0000-7000-8000-000000000001");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-08-18T01:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-08-18T02:00:00Z");

    private static final OffsetDateTime VERIFIED_AT = OffsetDateTime.parse("2026-08-18T01:30:00Z");

    @Mock private UserRepository userRepository;

    @Mock private UserProfileRepository userProfileRepository;

    @Mock private User user;

    @Mock private UserProfile profile;

    private UserProfileServiceImpl userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(userRepository, userProfileRepository);
    }

    @Test
    void getCurrentProfileShouldReturnSafeAccountAndProfileData() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));
        when(user.getId()).thenReturn(USER_ID);
        when(user.getEmail()).thenReturn("member@example.com");
        when(user.getUsername()).thenReturn("member");
        when(user.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(user.getEmailVerifiedAt()).thenReturn(VERIFIED_AT);
        when(user.getCreatedAt()).thenReturn(CREATED_AT);
        when(user.getUpdatedAt()).thenReturn(UPDATED_AT);
        when(profile.getFirstName()).thenReturn("Cinema");
        when(profile.getLastName()).thenReturn("Member");
        when(profile.getPhoneNumber()).thenReturn("+84901234567");

        CurrentUserProfileResponse response = userProfileService.getCurrentProfile(USER_ID);

        assertThat(response)
                .isEqualTo(
                        new CurrentUserProfileResponse(
                                USER_ID,
                                "member@example.com",
                                "member",
                                AccountStatus.ACTIVE,
                                "Cinema",
                                "Member",
                                "+84901234567",
                                VERIFIED_AT,
                                CREATED_AT,
                                UPDATED_AT));
    }

    @Test
    void getCurrentProfileShouldRejectMissingUserId() {
        assertThatThrownBy(() -> userProfileService.getCurrentProfile(null))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userRepository, userProfileRepository);
    }

    @Test
    void getCurrentProfileShouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getCurrentProfile(USER_ID))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void getCurrentProfileShouldFailWhenProfileDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getCurrentProfile(USER_ID))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findById(USER_ID);
        verify(userProfileRepository).findByUser_Id(USER_ID);
    }

    @Test
    void updateCurrentProfileShouldNormalizeAndApplyOwnedProfileData() {

        UpdateCurrentUserProfileRequest request =
                new UpdateCurrentUserProfileRequest("  Cinema  ", "  Member  ", "  +84901234567  ");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(userProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        when(user.getId()).thenReturn(USER_ID);
        when(profile.getFirstName()).thenReturn("Cinema");
        when(profile.getLastName()).thenReturn("Member");
        when(profile.getPhoneNumber()).thenReturn("+84901234567");

        CurrentUserProfileResponse response =
                userProfileService.updateCurrentProfile(USER_ID, request);

        verify(profile).update("Cinema", "Member", "+84901234567");

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.firstName()).isEqualTo("Cinema");
        assertThat(response.lastName()).isEqualTo("Member");
        assertThat(response.phoneNumber()).isEqualTo("+84901234567");
    }

    @Test
    void updateCurrentProfileShouldNormalizeBlankValuesToNull() {

        UpdateCurrentUserProfileRequest request =
                new UpdateCurrentUserProfileRequest(" ", null, "\t");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(userProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        userProfileService.updateCurrentProfile(USER_ID, request);

        verify(profile).update(null, null, null);
    }

    @Test
    void updateCurrentProfileShouldRejectMissingUserId() {

        UpdateCurrentUserProfileRequest request =
                new UpdateCurrentUserProfileRequest("Cinema", "Member", null);

        assertThatThrownBy(() -> userProfileService.updateCurrentProfile(null, request))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userRepository, userProfileRepository);
    }
}
