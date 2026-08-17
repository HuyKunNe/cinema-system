package com.cinema.user.oauth2.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.OAuth2ClientRegistrationService;
import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.RegisteredClientRegistrationResult;
import com.cinema.user.oauth2.model.ServiceClientRegistration;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OAuth2ClientRegistrationServiceImpl implements OAuth2ClientRegistrationService {

    private final RegisteredClientFactory registeredClientFactory;

    private final RegisteredClientRepository registeredClientRepository;

    private final SecurityAuditRecorder securityAuditRecorder;

    public OAuth2ClientRegistrationServiceImpl(
            RegisteredClientFactory registeredClientFactory,
            RegisteredClientRepository registeredClientRepository,
            SecurityAuditRecorder securityAuditRecorder) {

        this.registeredClientFactory = registeredClientFactory;

        this.registeredClientRepository = registeredClientRepository;

        this.securityAuditRecorder = securityAuditRecorder;
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerPublicClient(
            PublicClientRegistration registration) {

        rejectDuplicate(registration == null ? null : registration.clientId());

        RegisteredClient registeredClient =
                registeredClientFactory.createPublicClient(registration);

        save(registeredClient);
        recordRegistration(registeredClient, "PUBLIC");
        return toResult(registeredClient);
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerServiceClient(
            ServiceClientRegistration registration) {

        rejectDuplicate(registration == null ? null : registration.clientId());

        RegisteredClient registeredClient =
                registeredClientFactory.createServiceClient(registration);

        save(registeredClient);
        recordRegistration(registeredClient, "SERVICE");
        return toResult(registeredClient);
    }

    @Override
    @Transactional
    public RegisteredClientRegistrationResult registerConfidentialUserClient(
            ConfidentialUserClientRegistration registration) {

        rejectDuplicate(registration == null ? null : registration.clientId());

        RegisteredClient registeredClient =
                registeredClientFactory.createConfidentialUserClient(registration);

        save(registeredClient);
        recordRegistration(registeredClient, "CONFIDENTIAL_USER");
        return toResult(registeredClient);
    }

    private void recordRegistration(RegisteredClient registeredClient, String clientType) {

        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.OAUTH2_CLIENT_REGISTERED,
                        SecurityAuditTargetType.OAUTH2_CLIENT,
                        registeredClient.getClientId(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        "clientType=" + clientType));
    }

    private void rejectDuplicate(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        RegisteredClient existing = registeredClientRepository.findByClientId(clientId.trim());

        if (existing != null) {
            throw duplicateClient();
        }
    }

    private void save(RegisteredClient registeredClient) {

        try {
            registeredClientRepository.save(registeredClient);
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {

            throw new ConflictException(UserErrorCode.OAUTH2_CLIENT_ALREADY_EXISTS, exception);
        }
    }

    private static RegisteredClientRegistrationResult toResult(RegisteredClient registeredClient) {

        return new RegisteredClientRegistrationResult(
                registeredClient.getId(),
                registeredClient.getClientId(),
                registeredClient.getClientName());
    }

    private static ConflictException duplicateClient() {
        return new ConflictException(UserErrorCode.OAUTH2_CLIENT_ALREADY_EXISTS);
    }
}
