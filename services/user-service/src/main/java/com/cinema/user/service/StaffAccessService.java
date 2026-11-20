package com.cinema.user.service;

import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.enums.RoleName;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.StaffCinemaAssignmentRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StaffAccessService {

    private final UserRepository users;
    private final UserRoleRepository roles;
    private final StaffCinemaAssignmentRepository assignments;

    public StaffAccessService(
            UserRepository users,
            UserRoleRepository roles,
            StaffCinemaAssignmentRepository assignments) {
        this.users = users;
        this.roles = roles;
        this.assignments = assignments;
    }

    public Context current(UUID actor) {
        var user =
                users.findById(actor)
                        .orElseThrow(
                                () -> new ForbiddenException(UserErrorCode.STAFF_ACCESS_DENIED));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException(UserErrorCode.STAFF_ACCESS_DENIED);
        }
        var names =
                roles.findAllByUser_Id(actor).stream()
                        .map(assignment -> assignment.getRole().getName())
                        .toList();
        if (names.contains(RoleName.ADMIN)) {
            return new Context(actor, true, null);
        }
        if (!names.contains(RoleName.STAFF)) {
            throw new ForbiddenException(UserErrorCode.STAFF_ACCESS_DENIED);
        }
        var assignment =
                assignments
                        .findByUser_Id(actor)
                        .orElseThrow(
                                () -> new ForbiddenException(UserErrorCode.STAFF_ACCESS_DENIED));
        return new Context(actor, false, assignment.getCinemaId());
    }

    public record Context(UUID userId, boolean administrator, UUID cinemaId) {}
}
