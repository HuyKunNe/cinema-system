package com.cinema.user.security.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SecurityAuditAuthenticationFailureHandlerTest {

    private static final String USERNAME = "failed-user";

    private static final String PASSWORD = "password-must-not-appear";

    @Mock private SecurityAuditRecorder securityAuditRecorder;

    @Captor private ArgumentCaptor<SecurityAuditRecord> recordCaptor;

    @ParameterizedTest
    @MethodSource("authenticationFailures")
    void authenticationFailureShouldRecordBoundedReason(
            AuthenticationException exception, String expectedReason) throws Exception {

        SecurityAuditAuthenticationFailureHandler handler =
                new SecurityAuditAuthenticationFailureHandler(securityAuditRecorder);

        MockHttpServletRequest request = loginRequest(USERNAME);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        verify(securityAuditRecorder).record(recordCaptor.capture());

        SecurityAuditRecord record = recordCaptor.getValue();

        assertThat(record.eventType()).isEqualTo(SecurityAuditEventType.AUTHENTICATION_FAILED);

        assertThat(record.targetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(record.targetReference()).isEqualTo(USERNAME);

        assertThat(record.outcome()).isEqualTo(SecurityAuditOutcome.FAILURE);

        assertThat(record.reason()).isEqualTo(expectedReason);

        assertThat(record.metadata()).isNull();

        assertThat(record.toString())
                .doesNotContain(PASSWORD)
                .doesNotContain(exception.getMessage());

        assertThat(response.getStatus()).isEqualTo(302);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
    }

    @Test
    void oversizedUsernameShouldNotBePersistedAsTarget() throws Exception {

        SecurityAuditAuthenticationFailureHandler handler =
                new SecurityAuditAuthenticationFailureHandler(securityAuditRecorder);

        MockHttpServletRequest request = loginRequest("u".repeat(201));

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request, response, new BadCredentialsException("invalid credentials"));

        verify(securityAuditRecorder).record(recordCaptor.capture());

        SecurityAuditRecord record = recordCaptor.getValue();

        assertThat(record.targetType()).isNull();

        assertThat(record.targetReference()).isNull();

        assertThat(record.reason()).isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    void blankUsernameShouldNotBePersistedAsTarget() throws Exception {

        SecurityAuditAuthenticationFailureHandler handler =
                new SecurityAuditAuthenticationFailureHandler(securityAuditRecorder);

        MockHttpServletRequest request = loginRequest("   ");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request, response, new BadCredentialsException("invalid credentials"));

        verify(securityAuditRecorder).record(recordCaptor.capture());

        SecurityAuditRecord record = recordCaptor.getValue();

        assertThat(record.targetType()).isNull();

        assertThat(record.targetReference()).isNull();
    }

    private static MockHttpServletRequest loginRequest(String username) {

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setParameter("username", username);

        request.setParameter("password", PASSWORD);

        return request;
    }

    private static Stream<Arguments> authenticationFailures() {
        return Stream.of(
                Arguments.of(new LockedException("locked account detail"), "ACCOUNT_LOCKED"),
                Arguments.of(new DisabledException("disabled account detail"), "ACCOUNT_DISABLED"),
                Arguments.of(
                        new AccountExpiredException("expired account detail"), "ACCOUNT_EXPIRED"),
                Arguments.of(
                        new CredentialsExpiredException("expired credentials detail"),
                        "CREDENTIALS_EXPIRED"),
                Arguments.of(
                        new BadCredentialsException("bad credentials detail"), "BAD_CREDENTIALS"),
                Arguments.of(
                        new AuthenticationException("other authentication detail") {
                            private static final long serialVersionUID = 1L;
                        },
                        "AUTHENTICATION_FAILED"));
    }
}
