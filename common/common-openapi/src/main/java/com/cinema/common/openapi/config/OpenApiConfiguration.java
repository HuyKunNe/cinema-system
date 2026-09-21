package com.cinema.common.openapi.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import com.cinema.common.openapi.properties.OpenApiProperties;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@AutoConfiguration
@EnableConfigurationProperties(
        OpenApiProperties.class)
public class OpenApiConfiguration {

    public static final String OAUTH2 =
            "oauth2";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI cinemaOpenApi(
            OpenApiProperties properties) {

        OpenAPI openApi =
                new OpenAPI()
                        .info(
                                buildInfo(
                                        properties))
                        .components(
                                buildComponents(
                                        properties))
                        .addSecurityItem(
                                new SecurityRequirement()
                                        .addList(
                                                OAUTH2));

        List<Server> servers =
                buildServers(
                        properties);

        if (!servers.isEmpty()) {
            openApi.setServers(
                    servers);
        }

        return openApi;
    }

    private Components buildComponents(
            OpenApiProperties properties) {

        Scopes scopes =
                new Scopes()
                        .addString(
                                "openid",
                                "OpenID Connect")
                        .addString(
                                "profile",
                                "Read profile")
                        .addString(
                                "email",
                                "Read email")
                        .addString(
                                "booking:create",
                                "Create bookings")
                        .addString(
                                "booking:read",
                                "Read bookings")
                        .addString(
                                "booking:cancel",
                                "Cancel bookings")
                        .addString(
                                "movie:manage",
                                "Manage movies and genres")
                        .addString(
                                "showtime:manage",
                                "Manage showtimes")
                        .addString(
                                "inventory:manage",
                                "Manage cinema inventory")
                        .addString(
                                "payment:read",
                                "Read payments")
                        .addString(
                                "user:manage",
                                "Manage users");

        OAuthFlow authorizationCode =
                new OAuthFlow()
                        .authorizationUrl(
                                properties
                                        .getAuthorizationUrl())
                        .tokenUrl(
                                properties
                                        .getTokenUrl())
                        .scopes(
                                scopes);

        SecurityScheme oauth2 =
                new SecurityScheme()
                        .type(
                                SecurityScheme.Type.OAUTH2)
                        .flows(
                                new OAuthFlows()
                                        .authorizationCode(
                                                authorizationCode));

        return new Components()
                .addSecuritySchemes(
                        OAUTH2,
                        oauth2);
    }

    private Info buildInfo(
            OpenApiProperties properties) {

        Contact contact =
                new Contact()
                        .name(
                                properties
                                        .getContactName());

        if (StringUtils.hasText(
                properties.getContactEmail())) {

            contact.setEmail(
                    properties
                            .getContactEmail());
        }

        return new Info()
                .title(
                        properties.getTitle())
                .description(
                        properties.getDescription())
                .version(
                        properties.getVersion())
                .contact(
                        contact);
    }

    private List<Server> buildServers(
            OpenApiProperties properties) {

        List<Server> servers =
                new ArrayList<>();

        if (!StringUtils.hasText(
                properties.getServerUrl())) {

            return servers;
        }

        Server server =
                new Server()
                        .url(
                                properties
                                        .getServerUrl());

        if (StringUtils.hasText(
                properties
                        .getServerDescription())) {

            server.setDescription(
                    properties
                            .getServerDescription());
        }

        servers.add(
                server);

        return servers;
    }
}
