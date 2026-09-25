package com.cinema.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class UserCorsConfiguration {

    private static final List<String> ALLOWED_ORIGINS =
            List.of(
                    "http://localhost:5173",
                    "http://localhost:8081",
                    "http://localhost:8083",
                    "http://localhost:8084",
                    "http://localhost:8085");

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
    UrlBasedCorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
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
