package com.cinema.user.oauth2;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

public interface RegisteredClientFactory {

    RegisteredClient createPublicClient(
            PublicClientRegistration registration);

    RegisteredClient createServiceClient(
            ServiceClientRegistration registration);
}
