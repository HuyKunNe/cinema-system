package com.cinema.inventory.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class MovieCatalogClientConfiguration {

    @Bean("movieCatalogRequestFactory")
    JdkClientHttpRequestFactory movieCatalogRequestFactory(
            @Value("${cinema.inventory.movie-catalog.connect-timeout:2s}") Duration connectTimeout,
            @Value("${cinema.inventory.movie-catalog.read-timeout:3s}") Duration readTimeout) {

        if (connectTimeout.isZero()
                || connectTimeout.isNegative()
                || readTimeout.isZero()
                || readTimeout.isNegative()) {
            throw new IllegalArgumentException("Movie catalog timeouts must be greater than zero");
        }

        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);

        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Bean("movieCatalogRestClient")
    RestClient movieCatalogRestClient(
            RestClient.Builder builder,
            @Qualifier("movieCatalogRequestFactory") JdkClientHttpRequestFactory requestFactory,
            @Value("${cinema.inventory.movie-catalog.base-url}") URI baseUrl) {

        if (!("http".equalsIgnoreCase(baseUrl.getScheme())
                        || "https".equalsIgnoreCase(baseUrl.getScheme()))
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null
                || !(baseUrl.getPath().isEmpty() || "/".equals(baseUrl.getPath()))) {
            throw new IllegalArgumentException(
                    "Movie catalog base URL must contain only scheme, host and port");
        }

        return builder.clone().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build();
    }

    @Bean("movieCatalogAuthorizedClientManager")
    OAuth2AuthorizedClientManager movieCatalogAuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients,
            @Qualifier("movieCatalogRequestFactory") JdkClientHttpRequestFactory requestFactory) {

        RestClient tokenRestClient =
                RestClient.builder()
                        .requestFactory(requestFactory)
                        .messageConverters(
                                converters -> {
                                    converters.clear();
                                    converters.add(new FormHttpMessageConverter());
                                    converters.add(
                                            new OAuth2AccessTokenResponseHttpMessageConverter());
                                })
                        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                        .build();

        var tokenClient = new RestClientClientCredentialsTokenResponseClient();
        tokenClient.setRestClient(tokenRestClient);

        var provider = new ClientCredentialsOAuth2AuthorizedClientProvider();
        provider.setAccessTokenResponseClient(tokenClient);

        var manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        registrations, authorizedClients);

        manager.setAuthorizedClientProvider(provider);
        return manager;
    }
}
