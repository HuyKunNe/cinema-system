package com.cinema.inventory.config;

import com.cinema.common.outbox.config.OutboxConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(OutboxConfiguration.class)
public class InventoryOutboxConfiguration {}
