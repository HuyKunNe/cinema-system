package com.cinema.common.security.config;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.jwt.CinemaJwtGrantedAuthoritiesConverter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CinemaJwtGrantedAuthoritiesConverter cinemaJwtGrantedAuthoritiesConverter() {

        return new CinemaJwtGrantedAuthoritiesConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public CinemaJwtAuthenticationConverter cinemaJwtAuthenticationConverter(
            CinemaJwtGrantedAuthoritiesConverter authoritiesConverter) {

        return new CinemaJwtAuthenticationConverter(
                authoritiesConverter);
    }
}
