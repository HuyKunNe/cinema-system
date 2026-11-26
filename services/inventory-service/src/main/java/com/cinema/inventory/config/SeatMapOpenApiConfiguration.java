package com.cinema.inventory.config;

import com.cinema.inventory.enums.ShowSeatStatus;

import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

@Configuration
public class SeatMapOpenApiConfiguration {

    private static final String SEAT_SCHEMA_NAME = "ShowtimeSeatMapSeatResponse";

    @Bean
    public OpenApiCustomizer seatMapNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {

        return openApi -> {
            if (openApi.getComponents() == null
                    || openApi.getComponents().getSchemas() == null) {
                return;
            }

            boolean openApi31 = springDocProperties.isOpenapi31();

            configureAlwaysIncluded(
                    openApi.getComponents().getSchemas().get("ShowtimeSeatMapResponse"),
                    Set.of("canvasWidth", "canvasHeight", "matrix"),
                    openApi31);

            Schema<?> seatSchema =
                    openApi.getComponents().getSchemas().get(SEAT_SCHEMA_NAME);

            if (seatSchema == null) {
                return;
            }

            StringSchema showSeatIdSchema = new StringSchema();
            showSeatIdSchema.setFormat("uuid");
            showSeatIdSchema.setDescription(
                    "ShowSeat identifier; null when ShowSeat is missing.");

            NumberSchema priceSchema = new NumberSchema();
            priceSchema.setDescription(
                    "Price from ShowSeat; null when ShowSeat is missing.");

            StringSchema statusSchema = new StringSchema();
            statusSchema.setEnum(
                    Arrays.stream(ShowSeatStatus.values())
                            .map(ShowSeatStatus::name)
                            .toList());
            statusSchema.setDescription(
                    "Stored ShowSeat status; null when ShowSeat is missing.");

            seatSchema.addProperty(
                    "showSeatId", nullableSchema(showSeatIdSchema, openApi31));
            seatSchema.addProperty(
                    "price", nullableSchema(priceSchema, openApi31));
            seatSchema.addProperty(
                    "status", nullableSchema(statusSchema, openApi31));

            configureAlwaysIncluded(
                    seatSchema,
                    Set.of(
                            "showSeatId",
                            "price",
                            "status",
                            "x",
                            "y",
                            "width",
                            "height",
                            "rotationDegrees",
                            "rowIndex",
                            "columnIndex",
                            "number",
                            "columnSpan"),
                    openApi31);
        };
    }

    private static void configureAlwaysIncluded(
            Schema<?> schema, Set<String> nullableFields, boolean openApi31) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        var required = schema.getRequired() == null
                ? new ArrayList<String>()
                : new ArrayList<>(schema.getRequired());
        // The top-level response and seat records both use JsonInclude.ALWAYS.
        for (String field : schema.getProperties().keySet()) {
            if (!required.contains(field)) {
                required.add(field);
            }
            if (nullableFields.contains(field)) {
                schema.addProperty(field, nullableSchema(schema.getProperties().get(field), openApi31));
            }
        }
        schema.setRequired(required);
    }

    private static Schema<?> nullableSchema(
            Schema<?> valueSchema, boolean openApi31) {

        if (!openApi31) {
            if (valueSchema.get$ref() != null) {
                ComposedSchema result = new ComposedSchema();
                result.setDescription(valueSchema.getDescription());
                result.addAllOfItem(valueSchema);
                result.setNullable(true);
                return result;
            }
            valueSchema.setNullable(true);
            return valueSchema;
        }

        if (valueSchema.getAnyOf() != null
                && valueSchema.getAnyOf().stream().anyMatch(option ->
                        option.getTypes() != null && option.getTypes().contains("null"))) {
            return valueSchema;
        }

        valueSchema.setSpecVersion(SpecVersion.V31);

        Schema<Object> nullSchema = new Schema<>(SpecVersion.V31);
        nullSchema.setTypes(Set.of("null"));

        ComposedSchema result = new ComposedSchema();
        result.setSpecVersion(SpecVersion.V31);
        result.setDescription(valueSchema.getDescription());
        result.addAnyOfItem(valueSchema);
        result.addAnyOfItem(nullSchema);

        return result;
    }
}
