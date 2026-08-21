package com.cinema.common.kafka.serializer;

import com.cinema.common.kafka.event.BaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaEventSerializer {

    private final ObjectMapper objectMapper;

    public KafkaEventSerializer(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    public String serialize(BaseEvent event) {

        try {
            return objectMapper.writeValueAsString(event);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Kafka event", exception);
        }
    }
}
