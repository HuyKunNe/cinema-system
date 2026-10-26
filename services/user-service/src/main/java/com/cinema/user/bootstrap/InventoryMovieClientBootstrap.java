package com.cinema.user.bootstrap;

import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(
        prefix = "cinema.local-bootstrap",
        name = {"enabled", "inventory-movie-client-enabled"},
        havingValue = "true")
public class InventoryMovieClientBootstrap implements ApplicationRunner {

    private static final Set<String> SCOPES = Set.of("movie:catalog:read");

    private final RegisteredClientFactory factory;
    private final RegisteredClientRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String clientId;
    private final String clientSecret;

    public InventoryMovieClientBootstrap(
            RegisteredClientFactory factory,
            RegisteredClientRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${INVENTORY_MOVIE_CLIENT_ID:inventory-movie-catalog}") String clientId,
            @Value("${INVENTORY_MOVIE_CLIENT_SECRET}") String clientSecret) {

        this.factory = factory;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existing = repository.findByClientId(clientId);

        if (existing != null) {
            boolean valid =
                    existing.getClientAuthenticationMethods()
                                    .equals(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                            && existing.getAuthorizationGrantTypes()
                                    .equals(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                            && existing.getScopes().equals(SCOPES)
                            && existing.getClientSecret() != null
                            && passwordEncoder.matches(clientSecret, existing.getClientSecret());

            if (!valid) {
                throw new IllegalStateException(
                        "Existing Inventory movie client configuration does not match");
            }

            return;
        }

        repository.save(
                factory.createServiceClient(
                        new ServiceClientRegistration(
                                clientId, "Inventory movie catalog reader", clientSecret, SCOPES)));
    }
}
