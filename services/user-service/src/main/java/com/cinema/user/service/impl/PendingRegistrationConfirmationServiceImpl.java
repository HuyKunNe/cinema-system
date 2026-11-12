package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.PendingUserRegistrationRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.EmailVerificationTokenCodec;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.PendingRegistrationConfirmationService;
import com.cinema.user.service.UserCredentialService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PendingRegistrationConfirmationServiceImpl
        implements PendingRegistrationConfirmationService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final PendingUserRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserCredentialService userCredentialService;
    private final EmailVerificationTokenCodec tokenCodec;
    private final SecurityAuditRecorder securityAuditRecorder;
    private final Clock clock;

    public PendingRegistrationConfirmationServiceImpl(
            PendingUserRegistrationRepository pendingRegistrationRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            UserCredentialService userCredentialService,
            EmailVerificationTokenCodec tokenCodec,
            SecurityAuditRecorder securityAuditRecorder,
            Clock clock) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.userCredentialService = userCredentialService;
        this.tokenCodec = tokenCodec;
        this.securityAuditRecorder = securityAuditRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void confirm(String rawToken, String rawPassword) {
        if (rawToken == null || !TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw invalidToken();
        }

        String tokenHash = tokenCodec.hash(rawToken);
        var pending =
                pendingRegistrationRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(PendingRegistrationConfirmationServiceImpl::invalidToken);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!pending.isUsableAt(now)) {
            throw invalidToken();
        }
        if (userRepository.existsByNormalizedEmail(pending.getNormalizedEmail())) {
            throw invalidToken();
        }

        String username = "u" + UUID.randomUUID().toString().replace("-", "");
        User user =
                new User(
                        pending.getEmail(),
                        pending.getNormalizedEmail(),
                        username,
                        username.toLowerCase(Locale.ROOT));
        user.verifyEmail(now);
        user = userRepository.saveAndFlush(user);
        userCredentialService.createCredential(user.getId(), rawPassword);

        Role userRole =
                roleRepository
                        .findByName(RoleName.USER)
                        .orElseThrow(() ->
                                new IllegalStateException("USER role is not seeded by Flyway"));
        userRoleRepository.saveAndFlush(new UserRole(user, userRole, now, null));
        userProfileRepository.saveAndFlush(
                new UserProfile(
                        user,
                        pending.getFirstName(),
                        pending.getLastName(),
                        pending.getPhoneNumber()));

        pendingRegistrationRepository.delete(pending);
        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.USER_REGISTERED,
                        SecurityAuditTargetType.USER,
                        user.getId().toString(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        "method=password"));
    }

    private static UnauthorizedException invalidToken() {
        return new UnauthorizedException(UserErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
    }
}
