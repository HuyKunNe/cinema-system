package com.cinema.common.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonTestUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadJsonTree() {

        JsonNode result = JsonTestUtils.readTree(
                objectMapper,
                """
                        {
                          "name": "Cinema"
                        }
                        """);

        assertEquals(
                "Cinema",
                result.get("name").asText());

    }

    @Test
    void shouldTreatEquivalentJsonAsEqual() {

        String expected = """
                {
                  "name": "Cinema",
                  "active": true
                }
                """;

        String actual = """
                {
                  "active": true,
                  "name": "Cinema"
                }
                """;

        JsonTestUtils.assertJsonEquals(
                objectMapper,
                expected,
                actual);

    }

    @Test
    void shouldThrowWhenJsonIsDifferent() {

        String expected = """
                {
                  "name": "Cinema"
                }
                """;

        String actual = """
                {
                  "name": "Movie"
                }
                """;

        assertThrows(
                AssertionError.class,
                () -> JsonTestUtils.assertJsonEquals(
                        objectMapper,
                        expected,
                        actual));

    }

    @Test
    void shouldRejectInvalidJson() {

        assertThrows(
                IllegalArgumentException.class,
                () -> JsonTestUtils.readTree(
                        objectMapper,
                        "{invalid}"));

    }

    @Test
    void shouldSerializeAndDeserializeValue() {

        TestValue value = new TestValue(
                "booking-1");

        String json = JsonTestUtils.writeValue(
                objectMapper,
                value);

        TestValue result = JsonTestUtils.readValue(
                objectMapper,
                json,
                TestValue.class);

        assertEquals(
                value,
                result);

    }

    private record TestValue(
            String id) {

    }

}
