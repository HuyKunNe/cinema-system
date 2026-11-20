package com.cinema.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.user.client.StaffCinemaCatalogClient;
import com.cinema.user.dto.request.CreateStaffAccountRequest;
import com.cinema.user.entity.StaffCinemaAssignment;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.StaffCinemaAssignmentRepository;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.audit.SecurityAuditRecorder;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class AdministrativeStaffPersistenceTest {

    @Test
    void usesSavedUserForProfileAssignmentAndResponseWhenRepositoryMergesEntity() {
        var users = mock(UserRepository.class);
        var profiles = mock(UserProfileRepository.class);
        var assignments = mock(StaffCinemaAssignmentRepository.class);
        var credentials = mock(UserCredentialService.class);
        var roles = mock(UserRoleAssignmentService.class);
        var cinemas = mock(StaffCinemaCatalogClient.class);
        var audit = mock(SecurityAuditRecorder.class);
        UUID cinemaId = UUID.fromString("019c0000-0000-7000-8000-000000000001");
        UUID actorId = UUID.fromString("019c0000-0000-7000-8000-000000000002");
        var request =
                new CreateStaffAccountRequest(
                        "staff@example.invalid",
                        "unit-staff",
                        UUID.randomUUID().toString(),
                        "Staff",
                        "Account",
                        null,
                        cinemaId);
        User managed =
                User.provisionStaff(
                        request.email(), request.email(), request.username(), request.username());
        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User submitted = invocation.getArgument(0);
                    // JPA merge retains the identifier but may return a different instance.
                    ReflectionTestUtils.setField(managed, "id", submitted.getId());
                    return managed;
                });
        when(assignments.saveAndFlush(any(StaffCinemaAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var service =
                new AdministrativeStaffService(
                        users, profiles, assignments, credentials, roles, cinemas, audit);

        var response = service.create(request, actorId);

        ArgumentCaptor<User> submitted = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        ArgumentCaptor<StaffCinemaAssignment> assignment =
                ArgumentCaptor.forClass(StaffCinemaAssignment.class);
        verify(users).saveAndFlush(submitted.capture());
        verify(profiles).save(profile.capture());
        verify(assignments).saveAndFlush(assignment.capture());
        assertThat(managed).isNotSameAs(submitted.getValue());
        assertThat(managed.getId()).isEqualTo(submitted.getValue().getId());
        assertThat(profile.getValue().getUser()).isSameAs(managed);
        assertThat(assignment.getValue().getUser()).isSameAs(managed);
        assertThat(response.userId()).isEqualTo(managed.getId());
        assertThat(response.cinemaId()).isEqualTo(cinemaId);
        verify(credentials).createCredential(managed.getId(), request.password());
        verify(roles).assignRole(managed.getId(), RoleName.STAFF, actorId);
    }
}
