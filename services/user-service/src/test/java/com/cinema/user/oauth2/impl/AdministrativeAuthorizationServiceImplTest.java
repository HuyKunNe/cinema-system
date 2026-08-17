package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.entity.User;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdministrativeAuthorizationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString(
            "019c5000-0000-7000-8000-000000000601");

    private static final String USERNAME = "admin-target";

    private static final String REGISTERED_CLIENT_ID = "019c5000-0000-7000-8000-000000000602";

    private static final String CLIENT_ID = "inventory-service";

    private static final String NEW_RAW_CLIENT_SECRET = "new-administrative-client-secret";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private AuthorizationSessionRevocationService authorizationSessionRevocationService;

    @Mock
    private OAuth2ClientLifecycleService clientLifecycleService;

    @Mock
    private User user;

    private AdministrativeAuthorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdministrativeAuthorizationServiceImpl(
                userRepository,
                registeredClientRepository,
                authorizationSessionRevocationService,
                clientLifecycleService);
    }

    @Test
    void revokeUserAuthorizationsShouldResolveUsernameAndRevoke() {
        when(userRepository.findById(
                USER_ID))
                .thenReturn(
                        Optional.of(user));

        when(user.getUsername())
                .thenReturn(
                        USERNAME);

        service.revokeUserAuthorizations(
                USER_ID);

        verify(authorizationSessionRevocationService)
                .revokeByPrincipalName(
                        USERNAME,
                        RevocationReason.ADMIN_USER_REVOCATION);

        verifyNoInteractions(
                registeredClientRepository,
                clientLifecycleService);
    }

    @Test
    void revokeUserAuthorizationsShouldRejectMissingUser() {
        when(userRepository.findById(
                USER_ID))
                .thenReturn(
                        Optional.empty());

        assertThatThrownBy(() -> service.revokeUserAuthorizations(
                USER_ID))
                .isInstanceOf(
                        NotFoundException.class);

        verifyNoInteractions(
                registeredClientRepository,
                authorizationSessionRevocationService,
                clientLifecycleService);
    }

    @Test
    void revokeUserAuthorizationsShouldRejectNullUserId() {
        assertThatThrownBy(() -> service.revokeUserAuthorizations(
                null))
                .isInstanceOf(
                        ValidationException.class);

        verifyNoInteractions(
                userRepository,
                registeredClientRepository,
                authorizationSessionRevocationService,
                clientLifecycleService);
    }

    @Test
    void revokeClientAuthorizationsShouldResolveInternalIdAndRevoke() {
        RegisteredClient registeredClient = registeredClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(
                        registeredClient);

        service.revokeClientAuthorizations(
                CLIENT_ID);

        verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.ADMIN_CLIENT_REVOCATION);

        verifyNoInteractions(
                userRepository,
                clientLifecycleService);
    }

    @Test
    void revokeClientAuthorizationsShouldTrimClientId() {
        RegisteredClient registeredClient = registeredClient();

        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(
                        registeredClient);

        service.revokeClientAuthorizations(
                "  " + CLIENT_ID + "  ");

        verify(registeredClientRepository)
                .findByClientId(
                        CLIENT_ID);

        verify(authorizationSessionRevocationService)
                .revokeByRegisteredClientId(
                        REGISTERED_CLIENT_ID,
                        CLIENT_ID,
                        RevocationReason.ADMIN_CLIENT_REVOCATION);
    }

    @Test
    void revokeClientAuthorizationsShouldRejectMissingClient() {
        when(registeredClientRepository.findByClientId(
                CLIENT_ID))
                .thenReturn(
                        null);

        assertThatThrownBy(() -> service.revokeClientAuthorizations(
                CLIENT_ID))
                .isInstanceOf(
                        NotFoundException.class);

        verifyNoInteractions(
                userRepository,
                authorizationSessionRevocationService,
                clientLifecycleService);
    }

    @Test
    void revokeClientAuthorizationsShouldRejectBlankClientId() {
        assertThatThrownBy(() -> service.revokeClientAuthorizations(
                "   "))
                .isInstanceOf(
                        ValidationException.class);

        verifyNoInteractions(
                userRepository,
                registeredClientRepository,
                authorizationSessionRevocationService,
                clientLifecycleService);
    }

    @Test
    void deactivateClientShouldDelegate() {
        service.deactivateClient(
                CLIENT_ID);

        verify(clientLifecycleService)
                .deactivate(
                        CLIENT_ID);

        verifyNoInteractions(
                userRepository,
                registeredClientRepository,
                authorizationSessionRevocationService);
    }

    @Test
    void rotateClientSecretShouldDelegate() {
        service.rotateClientSecret(
                CLIENT_ID,
                NEW_RAW_CLIENT_SECRET);

        verify(clientLifecycleService)
                .rotateSecret(
                        CLIENT_ID,
                        NEW_RAW_CLIENT_SECRET);

        verifyNoInteractions(
                userRepository,
                registeredClientRepository,
                authorizationSessionRevocationService);
    }

    private static RegisteredClient registeredClient() {
        return RegisteredClient
                .withId(
                        REGISTERED_CLIENT_ID)
                .clientId(
                        CLIENT_ID)
                .clientSecret(
                        "{bcrypt}encoded-client-secret")
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
}
