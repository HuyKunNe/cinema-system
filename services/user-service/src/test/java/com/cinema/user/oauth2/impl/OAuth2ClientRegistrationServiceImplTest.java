package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.RegisteredClientRegistrationResult;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientRegistrationServiceImplTest {

    private static final String REGISTERED_CLIENT_ID = "019c4000-0000-7000-8000-000000000201";

    private static final String PUBLIC_CLIENT_ID = "cinema-web";

    private static final String PUBLIC_CLIENT_NAME = "Cinema Web";

    private static final String SERVICE_CLIENT_ID = "inventory-service";

    private static final String SERVICE_CLIENT_NAME = "Inventory Service";

    private static final String RAW_CLIENT_SECRET = "local-service-secret-for-testing-only";

    private static final String ENCODED_CLIENT_SECRET = "{bcrypt}encoded-service-secret";

    @Mock
    private RegisteredClientFactory registeredClientFactory;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    private OAuth2ClientRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuth2ClientRegistrationServiceImpl(
                registeredClientFactory,
                registeredClientRepository);
    }

    @Test
    void registerPublicClientShouldPersistFactoryResult() {
        PublicClientRegistration registration = publicRegistration();

        RegisteredClient registeredClient = publicRegisteredClient();

        when(registeredClientRepository
                .findByClientId(PUBLIC_CLIENT_ID))
                .thenReturn(null);

        when(registeredClientFactory
                .createPublicClient(registration))
                .thenReturn(registeredClient);

        RegisteredClientRegistrationResult result = service.registerPublicClient(registration);

        verify(registeredClientRepository)
                .save(registeredClient);

        assertThat(result.id())
                .isEqualTo(REGISTERED_CLIENT_ID);

        assertThat(result.clientId())
                .isEqualTo(PUBLIC_CLIENT_ID);

        assertThat(result.clientName())
                .isEqualTo(PUBLIC_CLIENT_NAME);
    }

    @Test
    void registerServiceClientShouldPersistFactoryResult() {
        ServiceClientRegistration registration = serviceRegistration();

        RegisteredClient registeredClient = serviceRegisteredClient();

        when(registeredClientRepository
                .findByClientId(SERVICE_CLIENT_ID))
                .thenReturn(null);

        when(registeredClientFactory
                .createServiceClient(registration))
                .thenReturn(registeredClient);

        RegisteredClientRegistrationResult result = service.registerServiceClient(registration);

        verify(registeredClientRepository)
                .save(registeredClient);

        assertThat(result.id())
                .isEqualTo(REGISTERED_CLIENT_ID);

        assertThat(result.clientId())
                .isEqualTo(SERVICE_CLIENT_ID);

        assertThat(result.clientName())
                .isEqualTo(SERVICE_CLIENT_NAME);
    }

    @Test
    void registerPublicClientShouldRejectExistingClientId() {
        PublicClientRegistration registration = publicRegistration();

        when(registeredClientRepository
                .findByClientId(PUBLIC_CLIENT_ID))
                .thenReturn(publicRegisteredClient());

        assertThatThrownBy(() -> service.registerPublicClient(registration))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(registeredClientFactory);

        verify(registeredClientRepository, never())
                .save(any());
    }

    @Test
    void registerServiceClientShouldRejectExistingClientId() {
        ServiceClientRegistration registration = serviceRegistration();

        when(registeredClientRepository
                .findByClientId(SERVICE_CLIENT_ID))
                .thenReturn(serviceRegisteredClient());

        assertThatThrownBy(() -> service.registerServiceClient(registration))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(registeredClientFactory);

        verify(registeredClientRepository, never())
                .save(any());
    }

    @Test
    void registerShouldTranslateRepositoryIllegalArgumentException() {
        PublicClientRegistration registration = publicRegistration();

        RegisteredClient registeredClient = publicRegisteredClient();

        when(registeredClientRepository
                .findByClientId(PUBLIC_CLIENT_ID))
                .thenReturn(null);

        when(registeredClientFactory
                .createPublicClient(registration))
                .thenReturn(registeredClient);

        doThrow(new IllegalArgumentException(
                "Registered client must be unique"))
                .when(registeredClientRepository)
                .save(registeredClient);

        assertThatThrownBy(() -> service.registerPublicClient(registration))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registerShouldTranslateDataIntegrityViolationException() {
        ServiceClientRegistration registration = serviceRegistration();

        RegisteredClient registeredClient = serviceRegisteredClient();

        when(registeredClientRepository
                .findByClientId(SERVICE_CLIENT_ID))
                .thenReturn(null);

        when(registeredClientFactory
                .createServiceClient(registration))
                .thenReturn(registeredClient);

        doThrow(new DataIntegrityViolationException(
                "duplicate client_id"))
                .when(registeredClientRepository)
                .save(registeredClient);

        assertThatThrownBy(() -> service.registerServiceClient(registration))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registrationResultShouldNotExposeClientSecret() {
        ServiceClientRegistration registration = serviceRegistration();

        RegisteredClient registeredClient = serviceRegisteredClient();

        when(registeredClientRepository
                .findByClientId(SERVICE_CLIENT_ID))
                .thenReturn(null);

        when(registeredClientFactory
                .createServiceClient(registration))
                .thenReturn(registeredClient);

        RegisteredClientRegistrationResult result = service.registerServiceClient(registration);

        assertThat(result.toString())
                .doesNotContain(RAW_CLIENT_SECRET)
                .doesNotContain(ENCODED_CLIENT_SECRET);
    }

    private static PublicClientRegistration publicRegistration() {

        return new PublicClientRegistration(
                PUBLIC_CLIENT_ID,
                PUBLIC_CLIENT_NAME,
                Set.of(
                        "http://127.0.0.1:3000/callback"),
                Set.of(
                        "http://127.0.0.1:3000"),
                Set.of(
                        "openid",
                        "profile",
                        "booking:read"));
    }

    private static ServiceClientRegistration serviceRegistration() {

        return new ServiceClientRegistration(
                SERVICE_CLIENT_ID,
                SERVICE_CLIENT_NAME,
                RAW_CLIENT_SECRET,
                Set.of("inventory:write"));
    }

    private static RegisteredClient publicRegisteredClient() {

        return RegisteredClient
                .withId(REGISTERED_CLIENT_ID)
                .clientId(PUBLIC_CLIENT_ID)
                .clientName(PUBLIC_CLIENT_NAME)
                .clientAuthenticationMethod(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "http://127.0.0.1:3000/callback")
                .scope("openid")
                .build();
    }

    private static RegisteredClient serviceRegisteredClient() {

        return RegisteredClient
                .withId(REGISTERED_CLIENT_ID)
                .clientId(SERVICE_CLIENT_ID)
                .clientSecret(ENCODED_CLIENT_SECRET)
                .clientName(SERVICE_CLIENT_NAME)
                .clientAuthenticationMethod(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("inventory:write")
                .build();
    }
}
