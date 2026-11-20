package com.cinema.user.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.user.client.StaffCinemaCatalogClient;
import com.cinema.user.dto.request.CreateStaffAccountRequest;
import com.cinema.user.dto.response.StaffAccountResponse;
import com.cinema.user.entity.StaffCinemaAssignment;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.enums.RoleName;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.StaffCinemaAssignmentRepository;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN') and hasAuthority('user:manage')")
public class AdministrativeStaffService {

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final StaffCinemaAssignmentRepository assignments;
    private final UserCredentialService credentials;
    private final UserRoleAssignmentService roles;
    private final StaffCinemaCatalogClient cinemas;
    private final SecurityAuditRecorder audit;

    public AdministrativeStaffService(
            UserRepository users,
            UserProfileRepository profiles,
            StaffCinemaAssignmentRepository assignments,
            UserCredentialService credentials,
            UserRoleAssignmentService roles,
            StaffCinemaCatalogClient cinemas,
            SecurityAuditRecorder audit) {
        this.users = users;
        this.profiles = profiles;
        this.assignments = assignments;
        this.credentials = credentials;
        this.roles = roles;
        this.cinemas = cinemas;
        this.audit = audit;
    }

    @Transactional
    public StaffAccountResponse create(CreateStaffAccountRequest request, UUID actor) {

        cinemas.requireActiveCinema(request.cinemaId());
        String email = request.email().trim();
        String username = request.username();

        if (users.existsByNormalizedEmail(normalize(email))
                || users.existsByNormalizedUsername(normalize(username))) {
            throw new ConflictException(UserErrorCode.STAFF_ACCOUNT_ALREADY_EXISTS);
        }

        User user = User.provisionStaff(email, normalize(email), username, normalize(username));

        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            throw new ConflictException(UserErrorCode.STAFF_ACCOUNT_ALREADY_EXISTS);
        }

        credentials.createCredential(user.getId(), request.password());

        profiles.save(
                new UserProfile(
                        user,
                        clean(request.firstName()),
                        clean(request.lastName()),
                        clean(request.phoneNumber())));

        var assignment =
                assignments.saveAndFlush(
                        new StaffCinemaAssignment(user, request.cinemaId(), actor));

        roles.assignRole(user.getId(), RoleName.STAFF, actor);

        record(
                SecurityAuditEventType.STAFF_ACCOUNT_CREATED,
                user.getId(),
                "cinemaId=" + request.cinemaId());

        return response(assignment);
    }

    public Page<StaffAccountResponse> list(UUID cinemaId, int page, int size) {

        var pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("createdAt").descending().and(Sort.by("id").descending()));

        return (cinemaId == null
                        ? assignments.findAll(pageable)
                        : assignments.findAllByCinemaId(cinemaId, pageable))
                .map(AdministrativeStaffService::response);
    }

    public StaffAccountResponse get(UUID userId) {
        return response(
                assignments
                        .findByUser_Id(userId)
                        .orElseThrow(() ->
                                new NotFoundException(UserErrorCode.STAFF_ASSIGNMENT_NOT_FOUND)));
    }

    @Transactional
    public StaffAccountResponse reassign(UUID userId, UUID cinemaId, UUID actor) {
        cinemas.requireActiveCinema(cinemaId);

        var assignment =
                assignments
                        .findByUserIdForUpdate(userId)
                        .orElseThrow(() ->
                                new NotFoundException(UserErrorCode.STAFF_ASSIGNMENT_NOT_FOUND));

        if (cinemaId.equals(assignment.getCinemaId())) {
            return response(assignment);
        }

        UUID previous = assignment.getCinemaId();
        assignment.reassign(cinemaId, actor);
        assignments.flush();

        record(
                SecurityAuditEventType.STAFF_CINEMA_CHANGED,
                userId,
                "previousCinemaId=" + previous + ";cinemaId=" + cinemaId);

        return response(assignment);
    }

    private void record(SecurityAuditEventType type, UUID userId, String metadata) {

        audit.record(
                new SecurityAuditRecord(
                        type,
                        SecurityAuditTargetType.USER,
                        userId.toString(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        metadata));
    }

    private static StaffAccountResponse response(StaffCinemaAssignment assignment) {

        User user = assignment.getUser();

        return new StaffAccountResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                assignment.getCinemaId(),
                assignment.getAssignedBy(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
