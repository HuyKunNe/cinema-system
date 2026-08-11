package com.cinema.user.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;

import com.cinema.user.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Jackson metadata used only by Spring Authorization Server's JDBC store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CinemaUserDetailsMixin {

    @JsonCreator
    CinemaUserDetailsMixin(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("status") AccountStatus status,
            @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    }
}
