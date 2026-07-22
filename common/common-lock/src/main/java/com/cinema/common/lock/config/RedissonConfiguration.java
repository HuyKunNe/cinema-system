package com.cinema.common.lock.config;

import org.redisson.api.RedissonClient;

import org.redisson.config.Config;

import org.redisson.Redisson;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedissonConfiguration {

    @Bean
    public RedissonClient redissonClient() {

        Config config = new Config();

        config.useSingleServer()
                .setAddress(
                        "redis://localhost:6379");

        return Redisson.create(config);

    }

}
