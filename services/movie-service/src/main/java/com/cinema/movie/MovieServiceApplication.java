package com.cinema.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@Import({ JpaAuditingConfiguration.class, GlobalExceptionHandler.class })
@SpringBootApplication
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                MovieServiceApplication.class,
                args);
    }
}
