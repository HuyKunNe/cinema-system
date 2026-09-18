package com.cinema.payment.config;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.kafka.consumer.KafkaConsumerConfiguration;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@Import(KafkaConsumerConfiguration.class)
@EnableConfigurationProperties(PaymentKafkaRetryProperties.class)
public class PaymentKafkaConsumerConfiguration {

    private static final String DEAD_LETTER_SUFFIX = ".dlt";

    @Bean
    DeadLetterPublishingRecoverer paymentDeadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (consumerRecord, exception) ->
                                new TopicPartition(
                                        consumerRecord.topic() + DEAD_LETTER_SUFFIX,
                                        consumerRecord.partition()));

        recoverer.setExceptionHeadersCreator(
                (headers, exception, isKey, headerNames) -> removeExceptionHeaders(headers));

        recoverer.setRetainExceptionHeader(false);

        recoverer.setAppendOriginalHeaders(false);

        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    @Bean
    CommonErrorHandler paymentKafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer, PaymentKafkaRetryProperties properties) {

        FixedBackOff backOff =
                new FixedBackOff(properties.interval().toMillis(), properties.maximumRetries());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        /*
         * Invalid contract data cannot become valid by retrying the same
         * Kafka record.
         */
        errorHandler.addNotRetryableExceptions(ValidationException.class);

        return errorHandler;
    }

    private static void removeExceptionHeaders(Headers headers) {

        headers.remove(KafkaHeaders.DLT_EXCEPTION_FQCN);

        headers.remove(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN);

        headers.remove(KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        headers.remove(KafkaHeaders.DLT_EXCEPTION_STACKTRACE);

        headers.remove(KafkaHeaders.DLT_KEY_EXCEPTION_FQCN);

        headers.remove(KafkaHeaders.DLT_KEY_EXCEPTION_MESSAGE);

        headers.remove(KafkaHeaders.DLT_KEY_EXCEPTION_STACKTRACE);
    }
}
