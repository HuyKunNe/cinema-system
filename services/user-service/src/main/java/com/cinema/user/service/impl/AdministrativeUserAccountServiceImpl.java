package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.AdministrativeUserAccountService;
import com.cinema.user.service.UserAccountLifecycleService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdministrativeUserAccountServiceImpl implements AdministrativeUserAccountService {

    private static final String USER_MANAGE = "hasAuthority('user:manage')";

    private final UserAccountLifecycleService userAccountLifecycleService;

    private final SecurityAuditRecorder securityAuditRecorder;

    public AdministrativeUserAccountServiceImpl(
            UserAccountLifecycleService userAccountLifecycleService,
            SecurityAuditRecorder securityAuditRecorder) {

        this.userAccountLifecycleService = userAccountLifecycleService;

        this.securityAuditRecorder = securityAuditRecorder;
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void lock(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.lock(userId);

        recordSuccess(SecurityAuditEventType.ACCOUNT_LOCKED, userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void unlock(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.unlock(userId);

        recordSuccess(SecurityAuditEventType.ACCOUNT_UNLOCKED, userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void disable(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.disable(userId);

        recordSuccess(SecurityAuditEventType.ACCOUNT_DISABLED, userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void enable(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.enable(userId);

        recordSuccess(SecurityAuditEventType.ACCOUNT_ENABLED, userId);
    }

    private void recordSuccess(SecurityAuditEventType eventType, UUID userId) {

        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        eventType,
                        SecurityAuditTargetType.USER,
                        userId.toString(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null));
    }

    private static void requireUserId(UUID userId) {

        if (userId == null) {
            throw new ValidationException(UserErrorCode.USER_ID_REQUIRED);
        }
    }
}
