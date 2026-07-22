package com.cinema.common.jackson.util;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new JacksonConfiguration().objectMapper();

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize object to JSON.", ex);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot deserialize JSON.", ex);
        }
    }

    public static <T> T convert(Object source, Class<T> clazz) {
        return MAPPER.convertValue(source, clazz);
    }
}
