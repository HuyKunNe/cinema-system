package com.cinema.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration(proxyBeanMethods = false)
public class RegisteredClientRepositoryConfiguration {

    @Bean
    RegisteredClientRepository registeredClientRepository(
            JdbcOperations jdbcOperations) {

        return new JdbcRegisteredClientRepository(
                jdbcOperations);
    }
}
