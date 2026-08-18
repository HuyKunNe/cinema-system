package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.UserAccountLifecycleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AdministrativeUserAccountServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("019ca000-0000-7000-8000-000000000001");

    @Mock private UserAccountLifecycleService userAccountLifecycleService;

    @Mock private SecurityAuditRecorder securityAuditRecorder;

    private AdministrativeUserAccountServiceImpl administrativeUserAccountService;

    @BeforeEach
    void setUp() {

        administrativeUserAccountService =
                new AdministrativeUserAccountServiceImpl(
                        userAccountLifecycleService, securityAuditRecorder);
    }

    @Test
    void lockShouldDelegateToLifecycleService() {

        administrativeUserAccountService.lock(USER_ID);

        verify(userAccountLifecycleService).lock(USER_ID);

        verifyAudit(SecurityAuditEventType.ACCOUNT_LOCKED);
    }

    @Test
    void unlockShouldDelegateToLifecycleService() {

        administrativeUserAccountService.unlock(USER_ID);

        verify(userAccountLifecycleService).unlock(USER_ID);

        verifyAudit(SecurityAuditEventType.ACCOUNT_UNLOCKED);
    }

    @Test
    void disableShouldDelegateToLifecycleService() {

        administrativeUserAccountService.disable(USER_ID);

        verify(userAccountLifecycleService).disable(USER_ID);

        verifyAudit(SecurityAuditEventType.ACCOUNT_DISABLED);
    }

    @Test
    void enableShouldDelegateToLifecycleService() {

        administrativeUserAccountService.enable(USER_ID);

        verify(userAccountLifecycleService).enable(USER_ID);

        verifyAudit(SecurityAuditEventType.ACCOUNT_ENABLED);
    }

    @Test
    void operationShouldRejectMissingUserId() {

        assertThatThrownBy(() -> administrativeUserAccountService.lock(null))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userAccountLifecycleService, securityAuditRecorder);
    }

    private void verifyAudit(SecurityAuditEventType eventType) {

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                eventType,
                                SecurityAuditTargetType.USER,
                                USER_ID.toString(),
                                SecurityAuditOutcome.SUCCESS,
                                null,
                                null));
    }
}
