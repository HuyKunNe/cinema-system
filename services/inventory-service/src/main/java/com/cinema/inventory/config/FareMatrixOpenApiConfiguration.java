package com.cinema.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class FareMatrixOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer fareMatrixNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            boolean openApi31 = springDocProperties.isOpenapi31();
            var schemas = openApi.getComponents().getSchemas();
            Schema<?> matrix = schemas.get("FareMatrixResponse");
            configure(
                    items(openApi, matrix, "ticketGroups"),
                    Set.of("eligibilityDescription"),
                    openApi31);
            configure(items(openApi, matrix, "cells"), Set.of("amount"), openApi31);
            configure(
                    items(openApi, schemas.get("FareMatrixValidationResponse"), "issues"),
                    Set.of("scope", "cell"),
                    openApi31);
        };
    }

    private static Schema<?> items(OpenAPI openApi, Schema<?> parent, String field) {
        if (parent == null || parent.getProperties() == null) {
            return null;
        }
        Schema<?> array = parent.getProperties().get(field);
        Schema<?> item = array == null ? null : array.getItems();
        if (item == null || item.get$ref() == null) {
            return item;
        }
        String prefix = "#/components/schemas/";
        return item.get$ref().startsWith(prefix)
                ? openApi.getComponents()
                        .getSchemas()
                        .get(item.get$ref().substring(prefix.length()))
                : null;
    }

    private static void configure(Schema<?> schema, Set<String> nullableFields, boolean openApi31) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        var required =
                schema.getRequired() == null
                        ? new ArrayList<String>()
                        : new ArrayList<>(schema.getRequired());
        // These nested records include all fields through JsonInclude.ALWAYS.
        for (String field : schema.getProperties().keySet()) {
            if (!required.contains(field)) {
                required.add(field);
            }
            if (nullableFields.contains(field)) {
                schema.addProperty(field, nullable(schema.getProperties().get(field), openApi31));
            }
        }
        schema.setRequired(required);
    }

    private static Schema<?> nullable(Schema<?> value, boolean openApi31) {
        if (!openApi31) {
            if (value.get$ref() != null) {
                ComposedSchema result = new ComposedSchema();
                result.setDescription(value.getDescription());
                result.addAllOfItem(value);
                result.setNullable(true);
                return result;
            }
            value.setNullable(true);
            return value;
        }
        if (value.getAnyOf() != null
                && value.getAnyOf().stream()
                        .anyMatch(option ->
                                option.getTypes() != null && option.getTypes().contains("null"))) {
            return value;
        }
        value.setSpecVersion(SpecVersion.V31);
        Schema<Object> nullSchema = new Schema<>(SpecVersion.V31);
        nullSchema.setTypes(Set.of("null"));
        ComposedSchema result = new ComposedSchema();
        result.setSpecVersion(SpecVersion.V31);
        result.setDescription(value.getDescription());
        result.addAnyOfItem(value);
        result.addAnyOfItem(nullSchema);
        return result;
    }
}
