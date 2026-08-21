package com.cinema.booking.config;

import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.config.OutboxConfiguration;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Import(OutboxConfiguration.class)
@ComponentScan(basePackageClasses = {OutboxService.class, KafkaProducerService.class})
@EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
public class BookingOutboxConfiguration {}
