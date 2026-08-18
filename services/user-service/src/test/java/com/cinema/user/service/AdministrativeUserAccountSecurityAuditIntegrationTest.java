package com.cinema.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.entity.User;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

class AdministrativeUserAccountSecurityAuditIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String ADMIN_USERNAME = "account-lifecycle-admin";

    private static final Set<SecurityAuditEventType> EXPECTED_EVENT_TYPES =
            Set.of(
                    SecurityAuditEventType.ACCOUNT_LOCKED,
                    SecurityAuditEventType.ACCOUNT_UNLOCKED,
                    SecurityAuditEventType.ACCOUNT_DISABLED,
                    SecurityAuditEventType.ACCOUNT_ENABLED);

    @Autowired private AdministrativeUserAccountService administrativeUserAccountService;

    @Autowired private UserRepository userRepository;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

    @Autowired private Clock clock;

    @Test
    @WithMockUser(username = ADMIN_USERNAME, authorities = "user:manage")
    void privilegedLifecycleOperationsShouldWriteSafeDurableAuditEvents() {

        User target = createActiveUser();

        administrativeUserAccountService.lock(target.getId());

        administrativeUserAccountService.unlock(target.getId());

        administrativeUserAccountService.disable(target.getId());

        administrativeUserAccountService.enable(target.getId());

        var events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.USER, target.getId().toString())
                        .stream()
                        .filter(event -> EXPECTED_EVENT_TYPES.contains(event.getEventType()))
                        .toList();

        assertThat(events).hasSize(4);

        assertThat(
                        events.stream()
                                .map(SecurityAuditEvent::getEventType)
                                .collect(Collectors.toSet()))
                .isEqualTo(EXPECTED_EVENT_TYPES);

        assertThat(events)
                .allSatisfy(
                        event -> {
                            assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

                            assertThat(event.getActorReference()).isEqualTo(ADMIN_USERNAME);

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.USER);

                            assertThat(event.getTargetReference())
                                    .isEqualTo(target.getId().toString());

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getReason()).isNull();

                            assertThat(event.getMetadata()).isNull();
                        });
    }

    private User createActiveUser() {

        String suffix = UUID.randomUUID().toString();

        String username = "audit-target-" + suffix;

        String email = username + "@example.com";

        User user = new User(email, email, username, username);

        user.verifyEmail(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        return userRepository.saveAndFlush(user);
    }
}
