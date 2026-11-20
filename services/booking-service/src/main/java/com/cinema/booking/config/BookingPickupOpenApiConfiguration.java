package com.cinema.booking.config;

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
public class BookingPickupOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer bookingPickupNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            boolean openApi31 = springDocProperties.isOpenapi31();
            var schemas = openApi.getComponents().getSchemas();
            configure(
                    schemas.get("BookingPickupPassResponse"),
                    Set.of("expiresAt", "qrPayload"),
                    openApi31);

            Schema<?> lookup = schemas.get("BookingPickupLookupResponse");
            configure(lookup, Set.of("totalGuests"), openApi31);
            Schema<?> tickets = property(lookup, "tickets");
            configure(
                    resolve(openApi, tickets == null ? null : tickets.getItems()),
                    Set.of("checkedInAt"),
                    openApi31);
            configure(
                    resolve(openApi, property(lookup, "printing")),
                    Set.of("simulationBlockReason"),
                    openApi31);
        };
    }

    private static void configure(Schema<?> schema, Set<String> nullableFields, boolean openApi31) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        var required =
                schema.getRequired() == null
                        ? new ArrayList<String>()
                        : new ArrayList<>(schema.getRequired());
        // These response records use JsonInclude.ALWAYS, including nullable fields.
        for (String field : schema.getProperties().keySet()) {
            if (!required.contains(field)) {
                required.add(field);
            }
        }
        schema.setRequired(required);
        for (String field : nullableFields) {
            Schema<?> value = property(schema, field);
            if (value != null) {
                schema.addProperty(field, nullable(value, openApi31));
            }
        }
    }

    private static Schema<?> property(Schema<?> schema, String name) {
        return schema == null || schema.getProperties() == null
                ? null
                : schema.getProperties().get(name);
    }

    private static Schema<?> resolve(OpenAPI openApi, Schema<?> reference) {
        String prefix = "#/components/schemas/";
        if (reference == null
                || reference.get$ref() == null
                || !reference.get$ref().startsWith(prefix)) {
            return null;
        }
        return openApi.getComponents()
                .getSchemas()
                .get(reference.get$ref().substring(prefix.length()));
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
