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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@AutoConfiguration
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI cinemaOpenApi(
            OpenApiProperties properties) {

        OpenAPI openApi = new OpenAPI()
                .info(
                        buildInfo(properties))
                .components(
                        buildComponents())
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(BEARER_AUTH));

        List<Server> servers = buildServers(properties);

        if (!servers.isEmpty()) {

            openApi.setServers(servers);

        }

        return openApi;

    }

    private Info buildInfo(
            OpenApiProperties properties) {

        Contact contact = new Contact()
                .name(
                        properties.getContactName());

        if (StringUtils.hasText(
                properties.getContactEmail())) {

            contact.setEmail(
                    properties.getContactEmail());

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

    private Components buildComponents() {

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(
                        SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "JWT access token");

        return new Components()
                .addSecuritySchemes(
                        BEARER_AUTH,
                        bearerScheme);

    }

    private List<Server> buildServers(
            OpenApiProperties properties) {

        List<Server> servers = new ArrayList<>();

        if (!StringUtils.hasText(
                properties.getServerUrl())) {

            return servers;

        }

        Server server = new Server()
                .url(
                        properties.getServerUrl());

        if (StringUtils.hasText(
                properties.getServerDescription())) {

            server.setDescription(
                    properties.getServerDescription());

        }

        servers.add(server);

        return servers;

    }

}
