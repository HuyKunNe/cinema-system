package com.cinema.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.User;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

class AdministrativeUserAccountAuditRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String AUDIT_FAILURE = "simulated account audit failure";

    @Autowired private AdministrativeUserAccountService administrativeUserAccountService;

    @Autowired private UserRepository userRepository;

    @Autowired private EntityManager entityManager;

    @Autowired private Clock clock;

    @MockitoBean private SecurityAuditRecorder securityAuditRecorder;

    @BeforeEach
    void failAuditRecording() {

        doThrow(new IllegalStateException(AUDIT_FAILURE))
                .when(securityAuditRecorder)
                .record(any(SecurityAuditRecord.class));
    }

    @Test
    @WithMockUser(username = "account-lifecycle-admin", authorities = "user:manage")
    void accountTransitionShouldRollbackWhenAuditRecordingFails() {

        User target = createActiveUser();

        assertThatThrownBy(() -> administrativeUserAccountService.lock(target.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE);

        entityManager.clear();

        User persisted = userRepository.findById(target.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        assertThat(persisted.getLockedAt()).isNull();
    }

    private User createActiveUser() {

        String suffix = UUID.randomUUID().toString();

        String username = "rollback-target-" + suffix;

        String email = username + "@example.com";

        User user = new User(email, email, username, username);

        user.verifyEmail(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        return userRepository.saveAndFlush(user);
    }
}
