package com.cinema.user.oauth2.impl;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class ActiveRegisteredClientRepository
        implements RegisteredClientRepository {

    private static final String ACTIVE_BY_ID_SQL = """
            SELECT active
            FROM oauth2_registered_client
            WHERE id = ?
            """;

    private static final String ACTIVE_BY_CLIENT_ID_SQL = """
            SELECT active
            FROM oauth2_registered_client
            WHERE client_id = ?
            """;

    private final RegisteredClientRepository delegate;

    private final JdbcOperations jdbcOperations;

    public ActiveRegisteredClientRepository(
            RegisteredClientRepository delegate,
            JdbcOperations jdbcOperations) {

        this.delegate = delegate;

        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public void save(
            RegisteredClient registeredClient) {

        delegate.save(
                registeredClient);
    }

    @Override
    public RegisteredClient findById(
            String id) {

        if (!isActive(
                ACTIVE_BY_ID_SQL,
                id)) {

            return null;
        }

        return delegate.findById(
                id);
    }

    @Override
    public RegisteredClient findByClientId(
            String clientId) {

        if (!isActive(
                ACTIVE_BY_CLIENT_ID_SQL,
                clientId)) {

            return null;
        }

        return delegate.findByClientId(
                clientId);
    }

    private boolean isActive(
            String sql,
            String value) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        return jdbcOperations.query(
                sql,
                resultSet -> resultSet.next()
                        && resultSet.getBoolean(
                                "active"),
                value);
    }
}
