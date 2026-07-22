package com.cinema.common.jackson;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonConfigurationTest {

    @Test
    void shouldCreateObjectMapper() {


        JacksonConfiguration configuration = new JacksonConfiguration();

        ObjectMapper mapper = configuration.objectMapper();

        assertNotNull(mapper);

    }

}
