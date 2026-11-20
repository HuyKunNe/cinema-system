package com.cinema.user.config;

import io.swagger.v3.oas.models.media.Schema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserCredentialsOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer userCredentialsCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            var schemas = openApi.getComponents().getSchemas();
            password(schemas.get("CreateStaffAccountRequest"), "password");
            password(schemas.get("ConfirmEmailVerificationRequest"), "password");
            password(schemas.get("ChangeCurrentUserPasswordRequest"), "currentPassword");
            password(schemas.get("ChangeCurrentUserPasswordRequest"), "newPassword");

            Schema<?> confirmation = schemas.get("ConfirmEmailVerificationRequest");
            if (confirmation != null && confirmation.getProperties() != null) {
                Schema<?> token = confirmation.getProperties().get("token");
                if (token != null) {
                    token.setWriteOnly(true);
                }
            }
        };
    }

    private static void password(Schema<?> schema, String field) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        Schema<?> property = schema.getProperties().get(field);
        if (property != null) {
            property.setFormat("password");
            property.setWriteOnly(true);
        }
    }
}
