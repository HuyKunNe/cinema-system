package com.cinema.user.oauth2.impl;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.oauth2.audit.RevocationReason;

@Service
@Transactional(readOnly = true)
public class OAuth2ClientLifecycleServiceImpl
        implements OAuth2ClientLifecycleService {

    private static final String DEACTIVATE_SQL = """
            UPDATE oauth2_registered_client
            SET active = FALSE
            WHERE id = ?
              AND active = TRUE
            """;

    private static final String ROTATE_SECRET_SQL = """
            UPDATE oauth2_registered_client
            SET client_secret = ?,
                client_secret_expires_at = NULL
            WHERE id = ?
              AND active = TRUE
            """;

    private final RegisteredClientRepository registeredClientRepository;

    private final AuthorizationSessionRevocationService authorizationSessionRevocationService;

    private final PasswordEncoder passwordEncoder;

    private final JdbcOperations jdbcOperations;

    public OAuth2ClientLifecycleServiceImpl(
            RegisteredClientRepository registeredClientRepository,
            AuthorizationSessionRevocationService authorizationSessionRevocationService,
            PasswordEncoder passwordEncoder,
            JdbcOperations jdbcOperations) {

        this.registeredClientRepository = registeredClientRepository;

        this.authorizationSessionRevocationService = authorizationSessionRevocationService;

        this.passwordEncoder = passwordEncoder;

        this.jdbcOperations = jdbcOperations;
    }

    @Override
    @Transactional
    public void deactivate(String clientId) {

        RegisteredClient client = findActiveClient(clientId);

        authorizationSessionRevocationService
                .revokeByRegisteredClientId(
                        client.getId(),
                        client.getClientId(),
                        RevocationReason.CLIENT_DEACTIVATED);

        int updated = jdbcOperations.update(
                DEACTIVATE_SQL,
                client.getId());

        if (updated != 1) {
            throw new ConflictException(UserErrorCode.OAUTH2_CLIENT_ALREADY_INACTIVE);
        }
    }

    @Override
    @Transactional
    public void rotateSecret(
            String clientId,
            String newRawClientSecret) {

        validateSecret(newRawClientSecret);

        RegisteredClient client = findActiveClient(clientId);

        if (!supportsClientSecret(client)) {

            throw new ConflictException(UserErrorCode.OAUTH2_CLIENT_SECRET_ROTATION_NOT_ALLOWED);
        }

        String encodedSecret = passwordEncoder.encode(newRawClientSecret);

        authorizationSessionRevocationService
                .revokeByRegisteredClientId(
                        client.getId(),
                        client.getClientId(),
                        RevocationReason.CLIENT_SECRET_ROTATED);

        int updated = jdbcOperations.update(
                ROTATE_SECRET_SQL,
                encodedSecret,
                client.getId());

        if (updated != 1) {
            throw new ConflictException(UserErrorCode.OAUTH2_CLIENT_ALREADY_INACTIVE);
        }
    }

    private RegisteredClient findActiveClient(String clientId) {

        if (clientId == null || clientId.isBlank()) {

            throw new ValidationException(UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        RegisteredClient client = registeredClientRepository
                .findByClientId(clientId.trim());

        if (client == null) {
            throw new NotFoundException(UserErrorCode.OAUTH2_CLIENT_NOT_FOUND);
        }

        return client;
    }

    private static void validateSecret(String rawClientSecret) {

        if (rawClientSecret == null || rawClientSecret.isBlank()) {

            throw new ValidationException(UserErrorCode.OAUTH2_CLIENT_SECRET_REQUIRED);
        }
    }

    private static boolean supportsClientSecret(
            RegisteredClient client) {

        return client
                .getClientAuthenticationMethods()
                .stream()
                .anyMatch(method -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                        .equals(method)
                        || ClientAuthenticationMethod.CLIENT_SECRET_POST
                                .equals(method));
    }
}
