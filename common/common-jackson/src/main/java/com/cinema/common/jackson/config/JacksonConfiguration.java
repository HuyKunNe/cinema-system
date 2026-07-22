package com.cinema.common.jackson.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {

        ObjectMapper mapper = JsonMapper.builder()

                .addModule(new JavaTimeModule())

                .serializationInclusion(JsonInclude.Include.NON_NULL)

                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)

                .build();

        mapper.findAndRegisterModules();

        return mapper;

    }

}
