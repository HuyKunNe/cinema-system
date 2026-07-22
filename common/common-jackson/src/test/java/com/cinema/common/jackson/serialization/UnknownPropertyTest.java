package com.cinema.common.jackson.serialization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.cinema.common.jackson.util.JsonUtils;

class UnknownPropertyTest {

    record MovieDto(

            Long id,

            String name

    ) {
    }

    @Test
    void shouldIgnoreUnknownProperty() {

        String json = """
                {
                  "id":1,
                  "name":"Avatar",
                  "abc":"xyz"
                }
                """;

        MovieDto dto = JsonUtils.fromJson(
                json,
                MovieDto.class);

        assertEquals(1L, dto.id());

        assertEquals("Avatar", dto.name());

    }

}
