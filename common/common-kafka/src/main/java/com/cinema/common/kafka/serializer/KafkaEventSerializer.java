package com.cinema.common.kafka.serializer;

import com.cinema.common.kafka.event.BaseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class KafkaEventSerializer {

    private final ObjectMapper objectMapper;

    public KafkaEventSerializer(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

    }

    public String serialize(
            BaseEvent event) {

        try {

            return objectMapper.writeValueAsString(
                    event);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Cannot serialize kafka event",
                    exception);

        }

    }

}
