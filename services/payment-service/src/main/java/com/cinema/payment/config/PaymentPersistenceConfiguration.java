package com.cinema.payment.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.repository.PaymentRepository;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {Payment.class, OutboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {PaymentRepository.class, OutboxRepository.class})
public class PaymentPersistenceConfiguration {}
