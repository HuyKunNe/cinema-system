package com.cinema.user.config;

import java.security.SecureRandom;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailVerificationProperties.class)
public class EmailVerificationConfiguration {

    @Bean
    SecureRandom emailVerificationSecureRandom() {
        return new SecureRandom();
    }
}
