package com.cinema.common.api.screening;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.util.Set;

public record ReplaceScreeningCapabilitiesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(using = StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull
                @Size(max = 32)
                Set<@NotNull ScreeningFormat> formats) {

    public static final class StrictLongDeserializer
            extends JsonDeserializer<Long> {

        @Override
        public Long deserialize(
                JsonParser parser,
                DeserializationContext context) throws IOException {

            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(
                        Long.class,
                        "expectedVersion must be a JSON integer");
            }

            return parser.getLongValue();
        }
    }
}
