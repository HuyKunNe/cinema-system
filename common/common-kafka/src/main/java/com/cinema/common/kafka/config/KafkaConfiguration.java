package com.cinema.common.kafka.config;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.kafka.producer.DefaultKafkaEventPublisher;
import com.cinema.common.kafka.producer.DefaultKafkaProducerService;
import com.cinema.common.kafka.serializer.KafkaEventSerializer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
    JacksonConfiguration.class,
    KafkaEventSerializer.class,
    DefaultKafkaProducerService.class,
    DefaultKafkaEventPublisher.class
})
public class KafkaConfiguration {}
