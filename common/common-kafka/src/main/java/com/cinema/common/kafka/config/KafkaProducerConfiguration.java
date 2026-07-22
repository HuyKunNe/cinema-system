package com.cinema.common.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class KafkaProducerConfiguration {

    public ProducerFactory<String, String> producerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                props);
    }

    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> factory) {

        return new KafkaTemplate<>(
                factory);

    }

}
