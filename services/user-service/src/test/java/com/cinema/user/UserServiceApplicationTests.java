package com.cinema.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceApplicationTests {

    @Test
    void contextShouldLoad(ApplicationContext applicationContext) {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void contextShouldLoadSharedInfrastructure(
            ApplicationContext applicationContext) {

        assertThat(
                applicationContext.getBean(GlobalExceptionHandler.class))
                .isNotNull();

        assertThat(
                applicationContext.getBean(JpaAuditingConfiguration.class))
                .isNotNull();

        assertThat(
                applicationContext.containsBean("auditingDateTimeProvider"))
                .isTrue();
    }
}
