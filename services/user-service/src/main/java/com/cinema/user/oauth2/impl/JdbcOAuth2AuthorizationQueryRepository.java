package com.cinema.user.oauth2.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

import com.cinema.user.oauth2.OAuth2AuthorizationQueryRepository;

@Repository
public class JdbcOAuth2AuthorizationQueryRepository
        implements OAuth2AuthorizationQueryRepository {

    private static final String FIND_IDS_BY_PRINCIPAL_NAME = """
            SELECT id
            FROM oauth2_authorization
            WHERE principal_name = ?
            ORDER BY id
            """;

    private static final String FIND_IDS_BY_REGISTERED_CLIENT_ID = """
            SELECT id
            FROM oauth2_authorization
            WHERE registered_client_id = ?
            ORDER BY id
            """;

    private final JdbcOperations jdbcOperations;

    public JdbcOAuth2AuthorizationQueryRepository(
            JdbcOperations jdbcOperations) {

        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public List<String> findIdsByPrincipalName(
            String principalName) {

        return jdbcOperations.queryForList(
                FIND_IDS_BY_PRINCIPAL_NAME,
                String.class,
                principalName);
    }

    @Override
    public List<String> findIdsByRegisteredClientId(
            String registeredClientId) {

        return jdbcOperations.queryForList(
                FIND_IDS_BY_REGISTERED_CLIENT_ID,
                String.class,
                registeredClientId);
    }
}
