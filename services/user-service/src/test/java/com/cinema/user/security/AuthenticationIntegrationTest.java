package com.cinema.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.UserCredentialService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.main.web-application-type=servlet",
            "cinema.user.authorization-server.issuer=http://localhost:8082"
        })
class AuthenticationIntegrationTest extends AbstractMySqlIntegrationTest {
    @Autowired private AuthenticationManager authenticationManager;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private UserRepository userRepository;

    @Autowired private UserCredentialRepository userCredentialRepository;

    @Autowired private UserRoleRepository userRoleRepository;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

    @Autowired private RoleRepository roleRepository;

    @Autowired private UserCredentialService userCredentialService;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private EntityManager entityManager;

    @Autowired private MockMvc mockMvc;

    private static final String RAW_PASSWORD = "correct-password-123";

    private static final OffsetDateTime ASSIGNED_AT = OffsetDateTime.parse("2026-08-07T03:00:00Z");

    @Test
    void shouldAuthenticateActiveUserFromDatabase() {
        User user = createUserWithCredential("active", AccountStatus.ACTIVE, RAW_PASSWORD);

        Authentication result = authenticate(user.getUsername(), RAW_PASSWORD);

        assertThat(result.isAuthenticated()).isTrue();

        assertThat(result.getPrincipal()).isInstanceOf(CinemaUserDetails.class);

        CinemaUserDetails principal = (CinemaUserDetails) result.getPrincipal();

        assertThat(principal.getUserId()).isEqualTo(user.getId());

        assertThat(principal.getUsername()).isEqualTo(user.getUsername());

        assertThat(result.getCredentials()).isNull();
        assertThat(principal.getPassword()).isNull();

        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "booking:cancel", "booking:create", "booking:read");
    }

    @Test
    void shouldAuthenticateUsingNormalizedEmail() {
        User user = createUserWithCredential("email-login", AccountStatus.ACTIVE, RAW_PASSWORD);

        Authentication result =
                authenticate("  " + user.getEmail().toUpperCase(Locale.ROOT) + "  ", RAW_PASSWORD);

        assertThat(result.isAuthenticated()).isTrue();

        assertThat(((CinemaUserDetails) result.getPrincipal()).getUserId()).isEqualTo(user.getId());
    }

    @Test
    void shouldRejectIncorrectPassword() {
        User user = createUserWithCredential("incorrect", AccountStatus.ACTIVE, RAW_PASSWORD);

        assertThatThrownBy(() -> authenticate(user.getUsername(), "incorrect-password-999"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldHideMissingUserAsBadCredentials() {
        assertThatThrownBy(() -> authenticate("missing-user", RAW_PASSWORD))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectLockedAccount() {
        User user = createUserWithCredential("locked", AccountStatus.LOCKED, RAW_PASSWORD);

        assertThatThrownBy(() -> authenticate(user.getUsername(), RAW_PASSWORD))
                .isInstanceOf(LockedException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountStatus.class,
            names = {"DISABLED", "PENDING_VERIFICATION"})
    void shouldRejectAccountWhenAuthenticationIsDisabled(AccountStatus status) {

        User user =
                createUserWithCredential(
                        status.name().toLowerCase(Locale.ROOT), status, RAW_PASSWORD);

        assertThatThrownBy(() -> authenticate(user.getUsername(), RAW_PASSWORD))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void shouldUpgradeLegacyBcryptHashAfterAuthentication() {
        User user = createUser("upgrade");

        String legacyHash = "{bcrypt}" + new BCryptPasswordEncoder(4).encode(RAW_PASSWORD);

        UserCredential credential =
                new UserCredential(user, legacyHash, "bcrypt", ASSIGNED_AT.minusDays(1));

        userCredentialRepository.saveAndFlush(credential);

        assignUserRole(user);
        updateStatus(user.getId(), AccountStatus.ACTIVE);

        entityManager.clear();

        Authentication result = authenticate(user.getUsername(), RAW_PASSWORD);

        assertThat(result.isAuthenticated()).isTrue();

        entityManager.flush();
        entityManager.clear();

        UserCredential updated = userCredentialRepository.findByUser_Id(user.getId()).orElseThrow();

        assertThat(updated.getPasswordHash()).isNotEqualTo(legacyHash).startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(RAW_PASSWORD, updated.getPasswordHash())).isTrue();

        assertThat(updated.getPasswordChangedAt()).isAfter(ASSIGNED_AT.minusDays(1));
    }

    @Test
    void shouldAuthenticateActiveUserThroughWebLogin() throws Exception {

        User user = createUserWithCredential("web-active", AccountStatus.ACTIVE, RAW_PASSWORD);

        mockMvc.perform(formLogin().user(user.getUsername()).password(RAW_PASSWORD))
                .andExpect(authenticated().withUsername(user.getUsername()));

        assertSuccessfulAuthenticationAudit(user.getUsername());
    }

    @Test
    void successfulLoginShouldChangeSessionIdentifier() throws Exception {

        User user = createUserWithCredential("web-session", AccountStatus.ACTIVE, RAW_PASSWORD);

        MockHttpSession originalSession = new MockHttpSession();

        String originalSessionId = originalSession.getId();

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .session(originalSession)
                                        .with(csrf())
                                        .param("username", user.getUsername())
                                        .param("password", RAW_PASSWORD))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(authenticated().withUsername(user.getUsername()))
                        .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) result.getRequest().getSession(false);

        assertThat(authenticatedSession).isNotNull();

        assertThat(authenticatedSession.getId()).isNotEqualTo(originalSessionId);
    }

    @Test
    void shouldRejectIncorrectPasswordThroughWebLogin() throws Exception {

        User user = createUserWithCredential("web-incorrect", AccountStatus.ACTIVE, RAW_PASSWORD);

        mockMvc.perform(formLogin().user(user.getUsername()).password("incorrect-password-999"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        assertFailedAuthenticationAudit(user.getUsername(), "BAD_CREDENTIALS");
    }

    @Test
    void shouldHideMissingUserThroughWebLogin() throws Exception {

        String attemptedUsername = "missing-web-user";

        mockMvc.perform(formLogin().user(attemptedUsername).password(RAW_PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        assertFailedAuthenticationAudit(attemptedUsername, "BAD_CREDENTIALS");
    }

    @Test
    void shouldRejectLockedAccountThroughWebLogin() throws Exception {

        User user = createUserWithCredential("web-locked", AccountStatus.LOCKED, RAW_PASSWORD);

        mockMvc.perform(formLogin().user(user.getUsername()).password(RAW_PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        assertFailedAuthenticationAudit(user.getUsername(), "ACCOUNT_LOCKED");
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountStatus.class,
            names = {"DISABLED", "PENDING_VERIFICATION"})
    void shouldRejectDisabledAuthenticationThroughWebLogin(AccountStatus status) throws Exception {

        User user =
                createUserWithCredential(
                        "web-" + status.name().toLowerCase(Locale.ROOT), status, RAW_PASSWORD);

        mockMvc.perform(formLogin().user(user.getUsername()).password(RAW_PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        assertFailedAuthenticationAudit(user.getUsername(), "ACCOUNT_DISABLED");
    }

    @Test
    void loginShouldRejectRequestWithoutCsrfToken() throws Exception {

        User user = createUserWithCredential("web-csrf", AccountStatus.ACTIVE, RAW_PASSWORD);

        mockMvc.perform(
                        post("/login")
                                .param("username", user.getUsername())
                                .param("password", RAW_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());

        assertThat(
                        securityAuditEventRepository
                                .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                        SecurityAuditTargetType.USER, user.getUsername()))
                .isEmpty();
    }

    private void assertSuccessfulAuthenticationAudit(String username) {

        SecurityAuditEvent event = singleAuthenticationAudit(username);

        assertThat(event.getEventType()).isEqualTo(SecurityAuditEventType.AUTHENTICATION_SUCCEEDED);

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(event.getActorReference()).isEqualTo(username);

        assertThat(event.getTargetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(event.getTargetReference()).isEqualTo(username);

        assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

        assertThat(event.getReason()).isNull();

        assertAuthenticationAuditSafe(event);
    }

    private void assertFailedAuthenticationAudit(String attemptedUsername, String expectedReason) {

        SecurityAuditEvent event = singleAuthenticationAudit(attemptedUsername);

        assertThat(event.getEventType()).isEqualTo(SecurityAuditEventType.AUTHENTICATION_FAILED);

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.SYSTEM);

        assertThat(event.getActorReference()).isEqualTo(CommonConstants.SYSTEM);

        assertThat(event.getTargetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(event.getTargetReference()).isEqualTo(attemptedUsername);

        assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.FAILURE);

        assertThat(event.getReason()).isEqualTo(expectedReason);

        assertAuthenticationAuditSafe(event);
    }

    private SecurityAuditEvent singleAuthenticationAudit(String username) {

        List<SecurityAuditEvent> events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.USER, username);

        assertThat(events).hasSize(1);

        return events.getFirst();
    }

    private static void assertAuthenticationAuditSafe(SecurityAuditEvent event) {

        assertThat(event.getMetadata()).isNull();

        assertThat(event.getCorrelationId())
                .isNotBlank()
                .hasSizeLessThanOrEqualTo(100)
                .doesNotContain(RAW_PASSWORD);

        assertThat(event.getActorReference()).doesNotContain(RAW_PASSWORD);

        assertThat(event.getTargetReference()).doesNotContain(RAW_PASSWORD);

        if (event.getReason() != null) {
            assertThat(event.getReason()).doesNotContain(RAW_PASSWORD);
        }

        assertThat(event.getOccurredAt()).isNotNull();

        assertThat(event.getCreatedAt()).isNotNull();

        assertThat(event.getUpdatedAt()).isNotNull();
    }

    private Authentication authenticate(String identifier, String rawPassword) {

        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(identifier, rawPassword));
    }

    private User createUserWithCredential(String prefix, AccountStatus status, String rawPassword) {

        User user = createUser(prefix);

        userCredentialService.createCredential(user.getId(), rawPassword);

        assignUserRole(user);
        updateStatus(user.getId(), status);

        entityManager.clear();

        return user;
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString();

        return userRepository.saveAndFlush(
                new User(
                        prefix + "." + suffix + "@example.com",
                        prefix + "." + suffix + "@example.com",
                        prefix + "." + suffix,
                        prefix + "." + suffix));
    }

    private void assignUserRole(User user) {
        Role role = roleRepository.findByName(RoleName.USER).orElseThrow();

        userRoleRepository.saveAndFlush(new UserRole(user, role, ASSIGNED_AT, user));
    }

    private void updateStatus(UUID userId, AccountStatus status) {

        int updated =
                jdbcTemplate.update(
                        """
                        UPDATE users
                        SET status = ?,
                            email_verified_at =
                                CASE
                                    WHEN ? = 'ACTIVE'
                                    THEN UTC_TIMESTAMP(6)
                                    ELSE email_verified_at
                                END
                        WHERE id = UNHEX(
                            REPLACE(?, '-', '')
                        )
                        """,
                        status.name(),
                        status.name(),
                        userId.toString());

        assertThat(updated).isEqualTo(1);
    }
}
