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

            Schema<?> seatSchema =
                    openApi.getComponents().getSchemas().get(SEAT_SCHEMA_NAME);

            if (seatSchema == null) {
                return;
            }

            boolean openApi31 = springDocProperties.isOpenapi31();

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
        };
    }

    private static Schema<?> nullableSchema(
            Schema<?> valueSchema, boolean openApi31) {

        if (!openApi31) {
            valueSchema.setNullable(true);
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
