package com.cinema.inventory.config;

import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EXCEPTION;
import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_CAUSE;
import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_MSG;
import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_STACKTRACE;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.kafka.consumer.KafkaConsumerConfiguration;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@Import(KafkaConsumerConfiguration.class)
@EnableConfigurationProperties(InventoryKafkaRetryProperties.class)
public class InventoryKafkaConsumerConfiguration {

    private static final String DEAD_LETTER_SUFFIX = ".dlt";

    @Bean
    DeadLetterPublishingRecoverer inventoryDeadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (consumerRecord, exception) ->
                                new TopicPartition(
                                        consumerRecord.topic() + DEAD_LETTER_SUFFIX,
                                        consumerRecord.partition()));

        recoverer.excludeHeader(EXCEPTION, EX_CAUSE, EX_MSG, EX_STACKTRACE);

        recoverer.setRetainExceptionHeader(false);
        recoverer.setStripPreviousExceptionHeaders(true);
        recoverer.setAppendOriginalHeaders(false);
        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    @Bean
    CommonErrorHandler inventoryKafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer, InventoryKafkaRetryProperties properties) {

        FixedBackOff backOff =
                new FixedBackOff(properties.interval().toMillis(), properties.maximumRetries());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(ValidationException.class);

        return errorHandler;
    }
}
