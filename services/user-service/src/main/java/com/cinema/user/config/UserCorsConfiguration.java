package com.cinema.user.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserCorsProperties.class)
public class UserCorsConfiguration {

    private static final List<String> ALLOWED_METHODS =
            List.of(
                    "GET",
                    "POST",
                    "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of(
                    "Authorization",
                    "Content-Type",
                    "Accept",
                    "Origin",
                    "X-Requested-With");

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            UserCorsProperties properties) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
