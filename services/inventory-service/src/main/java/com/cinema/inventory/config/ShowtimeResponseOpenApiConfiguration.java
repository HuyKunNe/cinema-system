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
public class ShowtimeResponseOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer showtimeResponseNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            var schemas = openApi.getComponents().getSchemas();
            boolean openApi31 = springDocProperties.isOpenapi31();
            Set<String> snapshotFields =
                    Set.of(
                            "screeningVersion",
                            "scheduling",
                            "pricingPolicyId",
                            "pricingPolicyVersionId");
            configure(schemas.get("ShowtimeResponse"), snapshotFields, snapshotFields, openApi31);

            Schema<?> screeningVersion = schemas.get("ShowtimeScreeningVersionResponse");
            if (screeningVersion != null && screeningVersion.getProperties() != null) {
                // The snapshot record includes every field, including a null subtitle.
                configure(
                        screeningVersion,
                        screeningVersion.getProperties().keySet(),
                        Set.of("subtitleLanguage"),
                        openApi31);
            }

            Schema<?> ticketPrices = schemas.get("ShowtimeTicketPricesResponse");
            Schema<?> groups = property(ticketPrices, "ticketGroups");
            Schema<?> ticketGroup = resolve(openApi, groups == null ? null : groups.getItems());
            configureAlwaysIncluded(ticketGroup, Set.of("eligibilityDescription"), openApi31);

            Schema<?> fares = property(ticketGroup, "fares");
            Schema<?> seatFare = resolve(openApi, fares == null ? null : fares.getItems());
            configureAlwaysIncluded(seatFare, Set.of("price"), openApi31);
        };
    }

    private static Schema<?> property(Schema<?> schema, String name) {
        return schema == null || schema.getProperties() == null
                ? null
                : schema.getProperties().get(name);
    }

    private static Schema<?> resolve(OpenAPI openApi, Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) {
            return schema;
        }
        String prefix = "#/components/schemas/";
        return schema.get$ref().startsWith(prefix)
                ? openApi.getComponents().getSchemas().get(schema.get$ref().substring(prefix.length()))
                : null;
    }

    private static void configureAlwaysIncluded(
            Schema<?> schema, Set<String> nullableFields, boolean openApi31) {
        if (schema != null && schema.getProperties() != null) {
            configure(schema, schema.getProperties().keySet(), nullableFields, openApi31);
        }
    }

    private static void configure(
            Schema<?> schema,
            Set<String> requiredFields,
            Set<String> nullableFields,
            boolean openApi31) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        var required =
                schema.getRequired() == null
                        ? new ArrayList<String>()
                        : new ArrayList<>(schema.getRequired());
        for (String field : schema.getProperties().keySet()) {
            if (requiredFields.contains(field) && !required.contains(field)) {
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
            // OpenAPI 3.0 ignores nullable siblings on a reference; wrap the reference.
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
