package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.audit.RevocationReason;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientLifecycleServiceImplTest {

    private static final String REGISTERED_CLIENT_ID = "019c5000-0000-7000-8000-000000000501";

    private static final String CLIENT_ID = "inventory-service";

    private static final String CURRENT_ENCODED_SECRET = "{bcrypt}current-secret";

    private static final String NEW_RAW_SECRET = "new-client-secret-for-testing";

    private static final String NEW_ENCODED_SECRET = "{bcrypt}new-secret";

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private AuthorizationSessionRevocationService authorizationSessionRevocationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcOperations jdbcOperations;

    private OAuth2ClientLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuth2ClientLifecycleServiceImpl(
                registeredClientRepository,
                authorizationSessionRevocationService,
                passwordEncoder,
                jdbcOperations);
    }

    @Test
    void deactivateShouldRevokeBeforeMarkingClientInactive() {
        RegisteredClient client = confidentialClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        when(jdbcOperations.update(
                anyString(),
                eq(REGISTERED_CLIENT_ID)))
                .thenReturn(1);

        service.deactivate(
                CLIENT_ID);

        InOrder order = inOrder(
                authorizationSessionRevocationService,
                jdbcOperations);

        order.verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.CLIENT_DEACTIVATED);

        order.verify(jdbcOperations)
                .update(
                        anyString(),
                        eq(REGISTERED_CLIENT_ID));

        verifyNoInteractions(
                passwordEncoder);
    }

    @Test
    void deactivateShouldTrimClientId() {
        RegisteredClient client = confidentialClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        when(jdbcOperations.update(
                anyString(),
                eq(REGISTERED_CLIENT_ID)))
                .thenReturn(1);

        service.deactivate(
                "  " + CLIENT_ID + "  ");

        verify(registeredClientRepository)
                .findByClientId(
                        CLIENT_ID);
    }

    @Test
    void deactivateShouldRejectMissingClient() {
        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(null);

        assertThatThrownBy(() -> service.deactivate(
                CLIENT_ID))
                .isInstanceOf(
                        NotFoundException.class);

        verifyNoInteractions(
                authorizationSessionRevocationService,
                passwordEncoder,
                jdbcOperations);
    }

    @Test
    void deactivateShouldRejectBlankClientIdBeforeLookup() {
        assertThatThrownBy(() -> service.deactivate(
                "   "))
                .isInstanceOf(
                        ValidationException.class);

        verifyNoInteractions(
                registeredClientRepository,
                authorizationSessionRevocationService,
                passwordEncoder,
                jdbcOperations);
    }

    @Test
    void deactivateShouldFailWhenConcurrentUpdateChangesNoRows() {
        RegisteredClient client = confidentialClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        when(jdbcOperations.update(
                anyString(),
                eq(REGISTERED_CLIENT_ID)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.deactivate(
                CLIENT_ID))
                .isInstanceOf(
                        ConflictException.class);

        verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.CLIENT_DEACTIVATED);
    }

    @Test
    void rotateSecretShouldRevokeBeforeUpdatingEncodedSecret() {
        RegisteredClient client = confidentialClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        when(passwordEncoder.encode(
                NEW_RAW_SECRET))
                .thenReturn(
                        NEW_ENCODED_SECRET);

        when(jdbcOperations.update(
                anyString(),
                eq(NEW_ENCODED_SECRET),
                eq(REGISTERED_CLIENT_ID)))
                .thenReturn(1);

        service.rotateSecret(
                CLIENT_ID,
                NEW_RAW_SECRET);

        InOrder order = inOrder(
                passwordEncoder,
                authorizationSessionRevocationService,
                jdbcOperations);

        order.verify(passwordEncoder)
                .encode(
                        NEW_RAW_SECRET);

        order.verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.CLIENT_SECRET_ROTATED);

        order.verify(jdbcOperations)
                .update(
                        anyString(),
                        eq(NEW_ENCODED_SECRET),
                        eq(REGISTERED_CLIENT_ID));
    }

    @Test
    void rotateSecretShouldRejectPublicClient() {
        RegisteredClient client = publicClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        assertThatThrownBy(() -> service.rotateSecret(
                CLIENT_ID,
                NEW_RAW_SECRET))
                .isInstanceOf(
                        ConflictException.class);

        verifyNoInteractions(
                passwordEncoder,
                authorizationSessionRevocationService,
                jdbcOperations);
    }

    @Test
    void rotateSecretShouldRejectMissingClient() {
        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(null);

        assertThatThrownBy(() -> service.rotateSecret(
                CLIENT_ID,
                NEW_RAW_SECRET))
                .isInstanceOf(
                        NotFoundException.class);

        verifyNoInteractions(
                passwordEncoder,
                authorizationSessionRevocationService,
                jdbcOperations);
    }

    @Test
    void rotateSecretShouldRejectBlankSecretBeforeLookup() {
        assertThatThrownBy(() -> service.rotateSecret(
                CLIENT_ID,
                "   "))
                .isInstanceOf(
                        ValidationException.class);

        verifyNoInteractions(
                registeredClientRepository,
                passwordEncoder,
                authorizationSessionRevocationService,
                jdbcOperations);
    }

    @Test
    void rotateSecretShouldFailWhenConcurrentUpdateChangesNoRows() {
        RegisteredClient client = confidentialClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(client);

        when(passwordEncoder.encode(
                NEW_RAW_SECRET))
                .thenReturn(
                        NEW_ENCODED_SECRET);

        when(jdbcOperations.update(
                anyString(),
                eq(NEW_ENCODED_SECRET),
                eq(REGISTERED_CLIENT_ID)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.rotateSecret(
                CLIENT_ID,
                NEW_RAW_SECRET))
                .isInstanceOf(
                        ConflictException.class);

        verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.CLIENT_SECRET_ROTATED);
    }

    private static RegisteredClient confidentialClient() {
        return RegisteredClient
                .withId(
                        REGISTERED_CLIENT_ID)
                .clientId(
                        CLIENT_ID)
                .clientSecret(
                        CURRENT_ENCODED_SECRET)
                .clientName(
                        "Inventory Service")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(
                        "inventory:read")
                .build();
    }

    private static RegisteredClient publicClient() {
        return RegisteredClient
                .withId(
                        REGISTERED_CLIENT_ID)
                .clientId(
                        CLIENT_ID)
                .clientName(
                        "Cinema Web")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "http://127.0.0.1:3000/callback")
                .scope(
                        "openid")
                .build();
    }
}
