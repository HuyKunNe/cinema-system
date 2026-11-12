package com.cinema.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableScheduling
@Import({
        JpaAuditingConfiguration.class,
        GlobalExceptionHandler.class
})
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                InventoryServiceApplication.class,
                args);
    }
}
