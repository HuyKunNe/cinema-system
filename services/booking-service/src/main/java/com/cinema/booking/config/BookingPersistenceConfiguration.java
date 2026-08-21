package com.cinema.booking.config;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {Booking.class, OutboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {BookingRepository.class, OutboxRepository.class})
public class BookingPersistenceConfiguration {}
