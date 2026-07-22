package com.cinema.common.security.jwt;

import java.util.Set;

public record JwtClaims(

        Long userId,

        String username,

        Set<String> roles,

        Set<String> permissions

) {

}
