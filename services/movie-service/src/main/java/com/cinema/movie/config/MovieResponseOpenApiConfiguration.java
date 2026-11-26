package com.cinema.movie.config;

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
public class MovieResponseOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer movieResponseNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            var schemas = openApi.getComponents().getSchemas();
            boolean openApi31 = springDocProperties.isOpenapi31();
            Set<String> movieFields = Set.of("backdropUrl", "ageRating");
            configure(schemas.get("MovieResponse"), movieFields, movieFields, openApi31);

            Set<String> heroFields = Set.of("heroStartsAt", "heroEndsAt");
            configure(
                    schemas.get("MovieHeroConfigurationResponse"),
                    heroFields,
                    heroFields,
                    openApi31);

            Schema<?> screeningVersion = schemas.get("MovieScreeningVersionResponse");
            if (screeningVersion != null && screeningVersion.getProperties() != null) {
                // This record includes all fields; the entity maps an empty subtitle to null.
                configure(
                        screeningVersion,
                        screeningVersion.getProperties().keySet(),
                        Set.of("subtitleLanguage"),
                        openApi31);
            }
        };
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
