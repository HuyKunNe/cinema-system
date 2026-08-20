package com.cinema.booking;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@Import({JpaAuditingConfiguration.class, GlobalExceptionHandler.class})
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
