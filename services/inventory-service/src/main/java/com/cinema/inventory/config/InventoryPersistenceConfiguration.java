package com.cinema.inventory.config;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.repository.ShowSeatRepository;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {ShowSeat.class, OutboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {ShowSeatRepository.class, OutboxRepository.class})
public class InventoryPersistenceConfiguration {}
