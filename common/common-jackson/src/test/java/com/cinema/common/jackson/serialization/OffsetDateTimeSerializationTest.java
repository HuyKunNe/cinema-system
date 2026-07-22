package com.cinema.common.jackson.serialization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.cinema.common.jackson.util.JsonUtils;

class OffsetDateTimeSerializationTest {

    record TestDto(

            OffsetDateTime createdAt

    ) {
    }

    @Test
    void shouldSerializeOffsetDateTimeAsIso8601() {

        TestDto dto = new TestDto(
                OffsetDateTime.parse(
                        "2026-07-22T10:15:30+07:00"));

        String json = JsonUtils.toJson(dto);

        assertTrue(
                json.contains(
                        "2026-07-22T10:15:30+07:00"));

    }

}
