package com.cinema.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    PaymentProperties.class,
    PaymentProviderOperationProperties.class,
    PaymentProviderWebhookProperties.class
})
public class PaymentConfiguration {}
