package com.cinema.common.security.authentication;

import java.util.Set;

public record AuthenticationUser(

        Long userId,

        String username,

        Set<String> roles,

        Set<String> permissions

) {

}
