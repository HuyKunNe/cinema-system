package com.cinema.common.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JacksonTest {

    @Test
    void shouldSerializeUuidAndDateTime()
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        String json = mapper.writeValueAsString(new TestDto(UUID.randomUUID(), OffsetDateTime.now()));

        assertNotNull(json);

        System.out.println(json);

    }

    record TestDto(
            UUID id,
            OffsetDateTime createdAt) {

    }

}
