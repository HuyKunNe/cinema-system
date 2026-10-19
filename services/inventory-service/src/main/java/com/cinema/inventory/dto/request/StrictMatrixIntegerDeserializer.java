package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public final class StrictMatrixIntegerDeserializer
        extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {

        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(
                    Integer.class,
                    "Matrix counts, indexes and seat numbers must be JSON integers");
        }

        return parser.getIntValue();
    }
}
