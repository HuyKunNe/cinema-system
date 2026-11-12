package com.cinema.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.SecureRandom;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
    EmailVerificationProperties.class,
    UserRegistrationProperties.class
})
public class EmailVerificationConfiguration {

    @Bean
    SecureRandom emailVerificationSecureRandom() {
        return new SecureRandom();
    }
}
