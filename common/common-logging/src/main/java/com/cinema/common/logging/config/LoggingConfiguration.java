package com.cinema.common.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.cinema.common.logging.aspect.LoggingAspect;

@Configuration
@EnableAspectJAutoProxy
public class LoggingConfiguration {
}
