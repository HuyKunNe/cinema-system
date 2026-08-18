package com.cinema.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

@SpringBootTest
class AdministrativeUserAccountSecurityIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("019ca000-0000-7000-8000-000000000001");

    @Autowired private AdministrativeUserAccountService administrativeUserAccountService;

    @MockitoBean private UserAccountLifecycleService userAccountLifecycleService;

    @Test
    @WithMockUser(authorities = "user:manage")
    void userManageAuthorityShouldAllowAccountLifecycleOperation() {

        assertThatCode(() -> administrativeUserAccountService.lock(USER_ID))
                .doesNotThrowAnyException();

        verify(userAccountLifecycleService).lock(USER_ID);
    }

    @Test
    @WithMockUser(authorities = "user:manage")
    void administrativeServiceShouldBeMethodSecurityProxy() {

        assertThat(AopUtils.isAopProxy(administrativeUserAccountService)).isTrue();
    }

    @Test
    @WithMockUser(authorities = "inventory:manage")
    void unrelatedAuthorityShouldBeDeniedBeforeLifecycleMutation() {

        assertThatThrownBy(() -> administrativeUserAccountService.disable(USER_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userAccountLifecycleService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleWithoutUserManageAuthorityShouldBeDenied() {

        assertThatThrownBy(() -> administrativeUserAccountService.unlock(USER_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userAccountLifecycleService);
    }

    @Test
    void unauthenticatedCallerShouldBeDeniedBeforeLifecycleMutation() {

        assertThatThrownBy(() -> administrativeUserAccountService.enable(USER_ID))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(userAccountLifecycleService);
    }
}
