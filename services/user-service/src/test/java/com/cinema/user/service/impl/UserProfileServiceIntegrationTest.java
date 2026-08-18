package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.dto.request.UpdateCurrentUserProfileRequest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserProfileService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserProfileServiceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private UserProfileService userProfileService;

    @Autowired private UserRepository userRepository;

    @Autowired private UserProfileRepository userProfileRepository;

    @Autowired private EntityManager entityManager;

    @Test
    void updateCurrentProfileShouldPersistOnlyTheOwnedProfile() {
        User owner = saveUser("owner");
        User other = saveUser("other");

        UserProfile ownerProfile =
                userProfileRepository.saveAndFlush(
                        new UserProfile(owner, "Before", "Owner", "+84111111111"));

        UserProfile otherProfile =
                userProfileRepository.saveAndFlush(
                        new UserProfile(other, "Other", "Member", "+84222222222"));

        CurrentUserProfileResponse response =
                userProfileService.updateCurrentProfile(
                        owner.getId(),
                        new UpdateCurrentUserProfileRequest(
                                "  After  ", "  Updated  ", "  +84333333333  "));

        entityManager.flush();
        entityManager.clear();

        UserProfile persistedOwner =
                userProfileRepository.findById(ownerProfile.getId()).orElseThrow();

        UserProfile persistedOther =
                userProfileRepository.findById(otherProfile.getId()).orElseThrow();

        assertThat(response.id()).isEqualTo(owner.getId());
        assertThat(response.firstName()).isEqualTo("After");
        assertThat(response.lastName()).isEqualTo("Updated");
        assertThat(response.phoneNumber()).isEqualTo("+84333333333");

        assertThat(persistedOwner.getFirstName()).isEqualTo("After");
        assertThat(persistedOwner.getLastName()).isEqualTo("Updated");
        assertThat(persistedOwner.getPhoneNumber()).isEqualTo("+84333333333");

        assertThat(persistedOther.getFirstName()).isEqualTo("Other");
        assertThat(persistedOther.getLastName()).isEqualTo("Member");
        assertThat(persistedOther.getPhoneNumber()).isEqualTo("+84222222222");
    }

    private User saveUser(String suffix) {
        return userRepository.saveAndFlush(
                new User(suffix + "@example.com", suffix + "@example.com", suffix, suffix));
    }
}
