package com.cinema.common.test.util;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonTestUtils {

    private JsonTestUtils() {

    }

    public static JsonNode readTree(
            ObjectMapper objectMapper,
            String json) {

        Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null");

        if (json == null || json.isBlank()) {

            throw new IllegalArgumentException(
                    "JSON must not be blank");

        }

        try {

            return objectMapper.readTree(
                    json);

        } catch (JsonProcessingException exception) {

            throw new IllegalArgumentException(
                    "Invalid JSON content",
                    exception);

        }

    }

    public static <T> T readValue(
            ObjectMapper objectMapper,
            String json,
            Class<T> targetType) {

        Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null");

        Objects.requireNonNull(
                targetType,
                "Target type must not be null");

        if (json == null || json.isBlank()) {

            throw new IllegalArgumentException(
                    "JSON must not be blank");

        }

        try {

            return objectMapper.readValue(
                    json,
                    targetType);

        } catch (JsonProcessingException exception) {

            throw new IllegalArgumentException(
                    "Cannot deserialize JSON to "
                            + targetType.getName(),
                    exception);

        }

    }

    public static String writeValue(
            ObjectMapper objectMapper,
            Object value) {

        Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null");

        Objects.requireNonNull(
                value,
                "Value must not be null");

        try {

            return objectMapper.writeValueAsString(
                    value);

        } catch (JsonProcessingException exception) {

            throw new IllegalArgumentException(
                    "Cannot serialize value of type "
                            + value.getClass().getName(),
                    exception);

        }

    }

    public static void assertJsonEquals(
            ObjectMapper objectMapper,
            String expected,
            String actual) {

        JsonNode expectedNode = readTree(
                objectMapper,
                expected);

        JsonNode actualNode = readTree(
                objectMapper,
                actual);

        if (!expectedNode.equals(
                actualNode)) {

            throw new AssertionError(
                    "JSON documents are different."
                            + System.lineSeparator()
                            + "Expected: "
                            + expectedNode.toPrettyString()
                            + System.lineSeparator()
                            + "Actual: "
                            + actualNode.toPrettyString());

        }

    }

}
