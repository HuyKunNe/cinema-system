package com.cinema.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({
    PaymentProperties.class,
    PaymentProviderOperationProperties.class,
    PaymentProviderWebhookProperties.class
})
public class PaymentConfiguration {}
