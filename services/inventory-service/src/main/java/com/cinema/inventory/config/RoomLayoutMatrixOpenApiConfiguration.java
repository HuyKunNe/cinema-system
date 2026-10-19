package com.cinema.inventory.config;

import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class RoomLayoutMatrixOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer roomLayoutMatrixNullabilityCustomizer(
            SpringDocConfigProperties springDocProperties) {

        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }

            var schemas = openApi.getComponents().getSchemas();
            boolean openApi31 = springDocProperties.isOpenapi31();

            configureSeatId(schemas.get("MatrixSeatResponse"), openApi31, true);

            configureSeatId(schemas.get("MatrixSeatInput"), openApi31, false);
        };
    }

    private static void configureSeatId(Schema<?> schema, boolean openApi31, boolean required) {

        if (schema == null) {
            return;
        }

        schema.addProperty("seatId", nullableUuid(openApi31));

        List<String> requiredFields =
                schema.getRequired() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(schema.getRequired());

        if (required) {
            if (!requiredFields.contains("seatId")) {
                requiredFields.add("seatId");
            }
        } else {
            requiredFields.removeIf("seatId"::equals);
        }

        schema.setRequired(requiredFields);
    }

    private static Schema<?> nullableUuid(boolean openApi31) {
        StringSchema valueSchema = new StringSchema();
        valueSchema.setFormat("uuid");
        valueSchema.setDescription(
                "Inventory seat identifier; null while the draft cell is unbound");

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
