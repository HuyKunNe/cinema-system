package com.cinema.user.oauth2.impl;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.entity.User;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.AdministrativeAuthorizationService;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AdministrativeAuthorizationServiceImpl
        implements AdministrativeAuthorizationService {

    private static final String USER_MANAGE = "hasAuthority('user:manage')";

    private final UserRepository userRepository;

    private final RegisteredClientRepository registeredClientRepository;

    private final AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private final OAuth2ClientLifecycleService clientLifecycleService;

    public AdministrativeAuthorizationServiceImpl(
            UserRepository userRepository,
            RegisteredClientRepository registeredClientRepository,
            AuthorizationSessionRevocationService authorizationSessionRevocationService,
            OAuth2ClientLifecycleService clientLifecycleService) {

        this.userRepository = userRepository;

        this.registeredClientRepository = registeredClientRepository;

        this.authorizationSessionRevocationService = authorizationSessionRevocationService;

        this.clientLifecycleService = clientLifecycleService;
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void revokeUserAuthorizations(
            UUID userId) {

        if (userId == null) {
            throw new ValidationException(
                    UserErrorCode.USER_ID_REQUIRED);
        }

        User user = userRepository.findById(
                userId)
                .orElseThrow(() -> new NotFoundException(
                        UserErrorCode.USER_NOT_FOUND));

        authorizationSessionRevocationService
                .revokeByPrincipalName(
                        user.getUsername());
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void revokeClientAuthorizations(
            String clientId) {

        RegisteredClient client = findActiveClient(
                clientId);

        authorizationSessionRevocationService
                .revokeByRegisteredClientId(
                        client.getId());
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void deactivateClient(
            String clientId) {

        clientLifecycleService.deactivate(
                clientId);
    }

    @Override
    @Transactional
    @PreAuthorize(USER_MANAGE)
    public void rotateClientSecret(
            String clientId,
            String newRawClientSecret) {

        clientLifecycleService.rotateSecret(
                clientId,
                newRawClientSecret);
    }

    private RegisteredClient findActiveClient(
            String clientId) {

        if (clientId == null
                || clientId.isBlank()) {

            throw new ValidationException(
                    UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        RegisteredClient client = registeredClientRepository
                .findByClientId(
                        clientId.trim());

        if (client == null) {
            throw new NotFoundException(
                    UserErrorCode.OAUTH2_CLIENT_NOT_FOUND);
        }

        return client;
    }
}
