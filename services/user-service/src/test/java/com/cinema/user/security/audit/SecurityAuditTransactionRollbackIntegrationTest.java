package com.cinema.user.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.Permission;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.oauth2.OAuth2ClientRegistrationService;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.repository.PermissionRepository;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.service.RolePermissionAssignmentService;
import com.cinema.user.service.UserRoleAssignmentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

class SecurityAuditTransactionRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String AUDIT_FAILURE_MESSAGE = "simulated durable security audit failure";

    @Autowired private UserRoleAssignmentService userRoleAssignmentService;

    @Autowired private RolePermissionAssignmentService rolePermissionAssignmentService;

    @Autowired private OAuth2ClientRegistrationService clientRegistrationService;

    @Autowired private OAuth2ClientLifecycleService clientLifecycleService;

    @Autowired private UserRepository userRepository;

    @Autowired private RoleRepository roleRepository;

    @Autowired private PermissionRepository permissionRepository;

    @Autowired private UserRoleRepository userRoleRepository;

    @Autowired private RolePermissionRepository rolePermissionRepository;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private Clock clock;

    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private SecurityAuditRecorder securityAuditRecorder;

    @BeforeEach
    void failSecurityAuditRecording() {
        doThrow(new IllegalStateException(AUDIT_FAILURE_MESSAGE))
                .when(securityAuditRecorder)
                .record(any(SecurityAuditRecord.class));
    }

    @Test
    void userRoleAssignmentShouldRollbackWhenAuditFails() {
        User actor = createActiveUser("role-rollback-actor");

        User target = createActiveUser("role-rollback-target");

        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();

        assertThat(userRoleRepository.existsByUser_IdAndRole_Id(target.getId(), role.getId()))
                .isFalse();

        assertThatThrownBy(
                        () ->
                                userRoleAssignmentService.assignRole(
                                        target.getId(), RoleName.ADMIN, actor.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        assertThat(userRoleRepository.existsByUser_IdAndRole_Id(target.getId(), role.getId()))
                .isFalse();
    }

    @Test
    void rolePermissionAssignmentShouldRollbackWhenAuditFails() {
        User actor = createActiveUser("permission-rollback-actor");

        Role role = roleRepository.findByName(RoleName.STAFF).orElseThrow();

        Permission permission =
                permissionRepository
                        .findByCode(PermissionCode.MOVIE_MANAGE.getCode())
                        .orElseThrow();

        normalizeRolePermissionAsAbsent(role, permission);

        assertThat(
                        rolePermissionRepository.existsByRole_IdAndPermission_Id(
                                role.getId(), permission.getId()))
                .isFalse();

        assertThatThrownBy(
                        () ->
                                rolePermissionAssignmentService.assignPermission(
                                        RoleName.STAFF, PermissionCode.MOVIE_MANAGE, actor.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        assertThat(
                        rolePermissionRepository.existsByRole_IdAndPermission_Id(
                                role.getId(), permission.getId()))
                .isFalse();
    }

    @Test
    void clientRegistrationShouldRollbackWhenAuditFails() {
        String clientId = "audit-rollback-public-" + UUID.randomUUID();

        PublicClientRegistration registration =
                new PublicClientRegistration(
                        clientId,
                        "Audit Rollback Public Client",
                        Set.of("http://127.0.0.1:3000/callback"),
                        Set.of("http://127.0.0.1:3000"),
                        Set.of("openid", "profile"));

        assertThat(registeredClientRepository.findByClientId(clientId)).isNull();

        assertThatThrownBy(() -> clientRegistrationService.registerPublicClient(registration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        assertThat(registeredClientRepository.findByClientId(clientId)).isNull();
    }

    @Test
    void clientDeactivationShouldRollbackWhenAuditFails() {
        RegisteredClient client = saveConfidentialClient();

        assertThat(registeredClientRepository.findByClientId(client.getClientId())).isNotNull();

        assertThatThrownBy(() -> clientLifecycleService.deactivate(client.getClientId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        assertThat(registeredClientRepository.findByClientId(client.getClientId())).isNotNull();

        Boolean active =
                jdbcTemplate.queryForObject(
                        """
                        SELECT active
                        FROM oauth2_registered_client
                        WHERE id = ?
                        """,
                        Boolean.class,
                        client.getId());

        assertThat(active).isTrue();
    }

    @Test
    void clientSecretRotationShouldRollbackWhenAuditFails() {
        RegisteredClient client = saveConfidentialClient();

        String previousSecret = client.getClientSecret();

        assertThatThrownBy(
                        () ->
                                clientLifecycleService.rotateSecret(
                                        client.getClientId(), "replacement-secret-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        RegisteredClient persisted =
                registeredClientRepository.findByClientId(client.getClientId());

        assertThat(persisted).isNotNull();

        assertThat(persisted.getClientSecret()).isEqualTo(previousSecret);

        assertThat(passwordEncoder.matches("replacement-secret-123", persisted.getClientSecret()))
                .isFalse();
    }

    private User createActiveUser(String prefix) {

        String suffix = UUID.randomUUID().toString();

        String username = prefix + "-" + suffix;

        String email = username + "@example.com";

        User user = new User(email, email.toLowerCase(), username, username.toLowerCase());

        user.verifyEmail(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        return userRepository.saveAndFlush(user);
    }

    private void normalizeRolePermissionAsAbsent(Role role, Permission permission) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(
                status -> {
                    rolePermissionRepository.deleteByRole_IdAndPermission_Id(
                            role.getId(), permission.getId());

                    rolePermissionRepository.flush();
                });
    }

    private RegisteredClient saveConfidentialClient() {
        String suffix = UUID.randomUUID().toString();

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("audit-rollback-client-" + suffix)
                        .clientIdIssuedAt(Instant.now(clock))
                        .clientSecret(passwordEncoder.encode("current-secret-123"))
                        .clientName("Audit Rollback Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("inventory:manage")
                        .build();

        registeredClientRepository.save(client);

        return client;
    }
}
