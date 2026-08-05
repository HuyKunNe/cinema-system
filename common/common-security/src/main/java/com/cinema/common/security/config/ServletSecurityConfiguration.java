package com.cinema.common.security.config;

import com.cinema.common.security.web.CinemaAccessDeniedHandler;
import com.cinema.common.security.web.CinemaAuthenticationEntryPoint;
import com.cinema.common.security.web.SecurityResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = SecurityConfiguration.class)
@ConditionalOnClass(name = {
        "jakarta.servlet.http.HttpServletRequest",
        "jakarta.servlet.http.HttpServletResponse"
})
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityResponseWriter securityResponseWriter(
            ObjectMapper objectMapper) {

        return new SecurityResponseWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CinemaAuthenticationEntryPoint
            cinemaAuthenticationEntryPoint(
                    SecurityResponseWriter responseWriter) {

        return new CinemaAuthenticationEntryPoint(
                responseWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public CinemaAccessDeniedHandler
            cinemaAccessDeniedHandler(
                    SecurityResponseWriter responseWriter) {

        return new CinemaAccessDeniedHandler(
                responseWriter);
    }
}
