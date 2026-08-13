package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.User;
import com.cinema.user.repository.UserRepository;

@SpringBootTest
@Transactional
class AdministrativeAuthorizationSecurityIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private AdministrativeAuthorizationService administrativeAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "platform-admin", authorities = "user:manage")
    void userManageAuthorityShouldAllowAdministrativeRevocation() {
        User user = saveUser(
                "allowed");

        assertThatCode(() -> administrativeAuthorizationService
                .revokeUserAuthorizations(
                        user.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(username = "platform-admin", authorities = "user:manage")
    void administrativeServiceShouldBeMethodSecurityProxy() {
        assertThat(AopUtils.isAopProxy(
                administrativeAuthorizationService))
                .isTrue();
    }

    @Test
    @WithMockUser(username = "staff", authorities = "inventory:manage")
    void unrelatedAuthorityShouldBeDeniedBeforeUserLookup() {
        UUID missingUserId = UUID.randomUUID();

        assertThatThrownBy(() -> administrativeAuthorizationService
                .revokeUserAuthorizations(
                        missingUserId))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "role-only-admin", roles = "ADMIN")
    void adminRoleWithoutUserManageAuthorityShouldBeDenied() {
        UUID missingUserId = UUID.randomUUID();

        assertThatThrownBy(() -> administrativeAuthorizationService
                .revokeUserAuthorizations(
                        missingUserId))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    @Test
    void unauthenticatedCallerShouldBeDeniedBeforeUserLookup() {
        UUID missingUserId = UUID.randomUUID();

        assertThatThrownBy(() -> administrativeAuthorizationService
                .revokeUserAuthorizations(
                        missingUserId))
                .isInstanceOf(
                        AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "staff", authorities = "inventory:manage")
    void unrelatedAuthorityShouldNotDeactivateClient() {
        assertThatThrownBy(() -> administrativeAuthorizationService
                .deactivateClient(
                        "missing-client"))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "staff", authorities = "inventory:manage")
    void unrelatedAuthorityShouldNotRotateClientSecret() {
        assertThatThrownBy(() -> administrativeAuthorizationService
                .rotateClientSecret(
                        "missing-client",
                        "new-client-secret"))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "staff", authorities = "inventory:manage")
    void unrelatedAuthorityShouldNotRevokeClientAuthorizations() {
        assertThatThrownBy(() -> administrativeAuthorizationService
                .revokeClientAuthorizations(
                        "missing-client"))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    private User saveUser(
            String prefix) {

        String suffix = UUID.randomUUID()
                .toString();

        String username = prefix
                + ".administrative."
                + suffix;

        String email = username
                + "@example.com";

        User user = new User(
                email,
                email.toLowerCase(),
                username,
                username.toLowerCase());

        return userRepository.saveAndFlush(
                user);
    }
}
