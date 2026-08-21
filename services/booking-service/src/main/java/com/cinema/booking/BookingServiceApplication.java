package com.cinema.booking;

import com.cinema.booking.entity.Booking;
import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EntityScan(basePackageClasses = {Booking.class, OutboxEventEntity.class})
@Import({JpaAuditingConfiguration.class, GlobalExceptionHandler.class})
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
