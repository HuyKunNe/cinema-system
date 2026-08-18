package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;
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

    public AdministrativeUserAccountServiceImpl(
            UserAccountLifecycleService userAccountLifecycleService) {

        this.userAccountLifecycleService = userAccountLifecycleService;
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void lock(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.lock(userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void unlock(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.unlock(userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void disable(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.disable(userId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void enable(UUID userId) {

        requireUserId(userId);

        userAccountLifecycleService.enable(userId);
    }

    private static void requireUserId(UUID userId) {

        if (userId == null) {
            throw new ValidationException(UserErrorCode.USER_ID_REQUIRED);
        }
    }
}
