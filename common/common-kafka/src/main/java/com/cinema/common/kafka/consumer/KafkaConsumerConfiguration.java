package com.cinema.common.kafka.consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration(proxyBeanMethods = false)
@EnableKafka
public class KafkaConsumerConfiguration {

}
