package com.cinema.common.outbox.config;

import com.cinema.common.kafka.config.KafkaConfiguration;
import com.cinema.common.outbox.acknowledgement.DefaultOutboxAcknowledgementService;
import com.cinema.common.outbox.claim.DefaultOutboxClaimService;
import com.cinema.common.outbox.publisher.KafkaOutboxPublisher;
import com.cinema.common.outbox.retry.OutboxRetryPolicy;
import com.cinema.common.outbox.scheduler.OutboxScheduler;
import com.cinema.common.outbox.service.DefaultOutboxService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.random.RandomGenerator;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@Import({
    KafkaConfiguration.class,
    DefaultOutboxService.class,
    DefaultOutboxClaimService.class,
    DefaultOutboxAcknowledgementService.class,
    OutboxRetryPolicy.class,
    KafkaOutboxPublisher.class,
    OutboxScheduler.class
})
public class OutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean(RandomGenerator.class)
    RandomGenerator outboxRandomGenerator() {

        return RandomGenerator.getDefault();
    }
}
