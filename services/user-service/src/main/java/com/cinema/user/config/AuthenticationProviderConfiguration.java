package com.cinema.user.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationProviderConfiguration {

    @Bean
    AuthenticationProvider userAuthenticationProvider(
            UserDetailsService userDetailsService,
            UserDetailsPasswordService userDetailsPasswordService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        provider.setUserDetailsPasswordService(
                userDetailsPasswordService);

        provider.setHideUserNotFoundExceptions(true);

        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationProvider userAuthenticationProvider) {

        return new ProviderManager(
                List.of(userAuthenticationProvider));
    }
}
