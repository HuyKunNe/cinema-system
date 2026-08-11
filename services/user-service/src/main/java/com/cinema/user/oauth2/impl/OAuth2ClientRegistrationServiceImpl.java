package com.cinema.user.oauth2.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.OAuth2ClientRegistrationService;
import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.RegisteredClientRegistrationResult;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

@Service
@Transactional(readOnly = true)
public class OAuth2ClientRegistrationServiceImpl
        implements OAuth2ClientRegistrationService {

    private final RegisteredClientFactory registeredClientFactory;

    private final RegisteredClientRepository registeredClientRepository;

    public OAuth2ClientRegistrationServiceImpl(
            RegisteredClientFactory registeredClientFactory,
            RegisteredClientRepository registeredClientRepository) {

        this.registeredClientFactory = registeredClientFactory;

        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerPublicClient(
            PublicClientRegistration registration) {

        rejectDuplicate(registration == null
                ? null
                : registration.clientId());

        RegisteredClient registeredClient = registeredClientFactory
                .createPublicClient(registration);

        save(registeredClient);

        return toResult(registeredClient);
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerServiceClient(
            ServiceClientRegistration registration) {

        rejectDuplicate(registration == null
                ? null
                : registration.clientId());

        RegisteredClient registeredClient = registeredClientFactory
                .createServiceClient(registration);

        save(registeredClient);

        return toResult(registeredClient);
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerConfidentialUserClient(
            ConfidentialUserClientRegistration registration) {

        rejectDuplicate(registration == null
                ? null
                : registration.clientId());

        RegisteredClient registeredClient = registeredClientFactory
                .createConfidentialUserClient(
                        registration);

        save(registeredClient);

        return toResult(registeredClient);
    }

    private void rejectDuplicate(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        RegisteredClient existing = registeredClientRepository
                .findByClientId(clientId.trim());

        if (existing != null) {
            throw duplicateClient();
        }
    }

    private void save(
            RegisteredClient registeredClient) {

        try {
            registeredClientRepository.save(
                    registeredClient);
        } catch (IllegalArgumentException
                | DataIntegrityViolationException exception) {

            throw new ConflictException(
                    UserErrorCode.OAUTH2_CLIENT_ALREADY_EXISTS,
                    exception);
        }
    }

    private static RegisteredClientRegistrationResult toResult(
            RegisteredClient registeredClient) {

        return new RegisteredClientRegistrationResult(
                registeredClient.getId(),
                registeredClient.getClientId(),
                registeredClient.getClientName());
    }

    private static ConflictException duplicateClient() {
        return new ConflictException(
                UserErrorCode.OAUTH2_CLIENT_ALREADY_EXISTS);
    }
}
