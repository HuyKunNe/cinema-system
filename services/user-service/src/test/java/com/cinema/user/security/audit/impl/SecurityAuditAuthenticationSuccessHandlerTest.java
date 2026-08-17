package com.cinema.user.security.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SecurityAuditAuthenticationSuccessHandlerTest {

    private static final String USERNAME = "authenticated-user";

    @Mock private SecurityAuditRecorder securityAuditRecorder;

    @Mock private Authentication authentication;

    @Captor private ArgumentCaptor<SecurityAuditRecord> recordCaptor;

    @Test
    void successfulAuthenticationShouldRecordAuditAndRedirect() throws Exception {

        when(authentication.getName()).thenReturn(USERNAME);

        SecurityAuditAuthenticationSuccessHandler handler =
                new SecurityAuditAuthenticationSuccessHandler(securityAuditRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(securityAuditRecorder).record(recordCaptor.capture());

        SecurityAuditRecord record = recordCaptor.getValue();

        assertThat(record.eventType()).isEqualTo(SecurityAuditEventType.AUTHENTICATION_SUCCEEDED);

        assertThat(record.targetType()).isEqualTo(SecurityAuditTargetType.USER);

        assertThat(record.targetReference()).isEqualTo(USERNAME);

        assertThat(record.outcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

        assertThat(record.reason()).isNull();

        assertThat(record.metadata()).isNull();

        assertThat(response.getStatus()).isEqualTo(302);

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}
